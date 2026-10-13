#!/usr/bin/env python3
"""Deploy the built WAR into a disposable, loopback-only Tomcat instance."""
import os
import http.cookiejar
import json
import pathlib
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import urllib.request
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parent.parent
TOMCAT = pathlib.Path(os.environ.get('CATALINA_HOME', '')).resolve()
WAR = pathlib.Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else ROOT / 'out/artifacts/guess-market.war'
if not (TOMCAT / 'bin/catalina.sh').is_file():
    sys.exit('Set CATALINA_HOME to a Tomcat 10.1 installation before running HTTP tests.')
with socket.socket() as sock:
    sock.bind(('127.0.0.1', 0))
    port = sock.getsockname()[1]
reports = ROOT / 'out/reports'
reports.mkdir(parents=True, exist_ok=True)
with tempfile.TemporaryDirectory(prefix='http-test-', dir=ROOT / 'out') as tmp:
    base = pathlib.Path(tmp)
    shutil.copytree(TOMCAT / 'conf', base / 'conf')
    for folder in ['logs', 'webapps', 'work', 'temp']:
        (base / folder).mkdir()
    config = ET.parse(base / 'conf/server.xml')
    config.getroot().set('port', '-1')
    service = config.getroot().find('Service')
    for connector in list(service.findall('Connector')):
        service.remove(connector)
    ET.SubElement(service, 'Connector', {'address':'127.0.0.1', 'port':str(port),
        'protocol':'HTTP/1.1', 'connectionTimeout':'20000'})
    config.write(base / 'conf/server.xml', encoding='UTF-8', xml_declaration=True)
    shutil.copyfile(WAR, base / 'webapps/guess-market.war')
    env = dict(os.environ, CATALINA_BASE=str(base), CATALINA_HOME=str(TOMCAT))
    env['GUESS_MARKET_ALLOWED_HOSTS'] = 'localhost,127.0.0.1,::1,guess-market.test'
    url = f'http://127.0.0.1:{port}/guess-market/api'
    with (reports / 'tomcat-http.log').open('w') as log:
        server = subprocess.Popen([str(TOMCAT / 'bin/catalina.sh'), 'run'], env=env,
            stdout=log, stderr=subprocess.STDOUT)
        try:
            for _ in range(100):
                if server.poll() is not None:
                    raise RuntimeError('Tomcat exited; see out/reports/tomcat-http.log')
                try:
                    with urllib.request.urlopen(url + '/health', timeout=1) as response:
                        if response.status == 200:
                            break
                except (OSError, urllib.error.URLError):
                    time.sleep(.1)
            else:
                raise RuntimeError('Tomcat did not become ready; see out/reports/tomcat-http.log')
            # The course grader cannot write uploaded files. Prove uploads need no temp files.
            upload_directories = [base / 'temp', base / 'work/Catalina/localhost/guess-market']
            for directory in upload_directories:
                if not directory.is_dir():
                    raise AssertionError('Tomcat upload directory missing: ' + str(directory))
                directory.chmod(0o555)
            original_files = {directory: set(directory.rglob('*')) for directory in upload_directories}
            saved_state = reports / 'exercise-3-state.gmstate'
            subprocess.run([sys.executable, str(ROOT / 'tests/http_integration.py'), url,
                            str(saved_state)], check=True)
            classpath = os.pathsep.join(str(ROOT / path) for path in [
                'out/test-classes', 'out/artifacts/client/client.jar', 'out/artifacts/client/dto.jar',
                'out/artifacts/client/lib/gson-2.13.2.jar'])
            subprocess.run(['java', '-ea', '-cp', classpath, 'guessmarket.tests.ApiClientIntegrationTest', url], check=True)
            for directory in upload_directories:
                if set(directory.rglob('*')) != original_files[directory]:
                    raise AssertionError('An upload wrote to ' + str(directory))
            print('PASS: uploads work with read-only server and servlet temp directories')
        finally:
            for directory in [base / 'temp', base / 'work/Catalina/localhost/guess-market']:
                if directory.is_dir():
                    directory.chmod(0o755)
            server.terminate()
            try:
                server.wait(timeout=10)
            except subprocess.TimeoutExpired:
                server.kill()
                server.wait()

    # Reuse the same deployment directory across a process restart: nothing may survive.
    with (reports / 'tomcat-restart.log').open('w') as log:
        server = subprocess.Popen([str(TOMCAT / 'bin/catalina.sh'), 'run'], env=env,
            stdout=log, stderr=subprocess.STDOUT)
        try:
            for _ in range(100):
                try:
                    with urllib.request.urlopen(url + '/health', timeout=1) as response:
                        if response.status == 200:
                            break
                except OSError:
                    time.sleep(.1)
            else:
                raise RuntimeError('Restarted Tomcat did not become ready')
            cookies = http.cookiejar.CookieJar()
            client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies))
            request = urllib.request.Request(url + '/login', data=b'{"username":"Market Maker"}',
                headers={'Content-Type':'application/json'})
            with client.open(request, timeout=5) as response:
                assert response.status == 200, 'Previously reserved name survived server restart'
            with client.open(url + '/snapshot', timeout=5) as response:
                fresh = json.load(response)
            assert fresh['events'] == [] and fresh['chat'] == [] and fresh['self']['balance'] == 0
            assert len(fresh['users']) == 1, 'User directory survived server restart'
            boundary = 'GuessMarketRestartState'
            contents = saved_state.read_bytes()
            body = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="restart.gmstate"\r\n'
                    'Content-Type: application/octet-stream\r\n\r\n').encode() + contents + f'\r\n--{boundary}--\r\n'.encode()
            restore = urllib.request.Request(url + '/state', data=body, method='POST', headers={
                'Content-Type': 'multipart/form-data; boundary=' + boundary})
            with client.open(restore, timeout=5) as response:
                result = json.load(response)
            assert result['eventCount'] > 0 and result['userCount'] > 1
            newcomer = urllib.request.build_opener(
                urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
            claim = urllib.request.Request(url + '/login', data=b'{"username":"Trader"}',
                headers={'Content-Type':'application/json'})
            with newcomer.open(claim, timeout=5) as response:
                assert response.status == 200, 'A restored, unclaimed Exercise 3 user could not reconnect'
            with newcomer.open(url + '/snapshot', timeout=5) as response:
                restored = json.load(response)
            assert restored['self']['name'] == 'Trader' and restored['events']
            assert len(restored['accountEntries']) > 1, 'Restored user operation history is missing'
            print('PASS: restart starts empty and a manual Exercise 3 state restore recovers users and history')
        finally:
            server.terminate()
            try:
                server.wait(timeout=10)
            except subprocess.TimeoutExpired:
                server.kill()
                server.wait()
