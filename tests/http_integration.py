#!/usr/bin/env python3
"""Black-box Exercise 3 checks against a fresh deployed WAR (standard library only)."""
import concurrent.futures
import base64
import http.cookiejar
import json
import math
import pathlib
import sys
import urllib.error
import urllib.request
import uuid

BASE = (sys.argv[1] if len(sys.argv) > 1 else 'http://localhost:18080/guess-market/api').rstrip('/')
ROOT = pathlib.Path(__file__).resolve().parent.parent
STATE_OUTPUT = pathlib.Path(sys.argv[2]) if len(sys.argv) > 2 else None
checks = 0


def check(condition, message):
    global checks
    if not condition:
        raise AssertionError(message)
    checks += 1


class Client:
    def __init__(self):
        self.jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))

    def request(self, path, body=None, method=None, raw=None, content_type=None,
                headers=None, return_headers=False, binary=False):
        data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
        request_headers = dict(headers or {})
        if data is not None:
            request_headers.setdefault('Content-Type', content_type or 'application/json')
        request = urllib.request.Request(BASE + path, data=data, method=method,
            headers=request_headers)
        try:
            response = self.opener.open(request, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            payload = response.read()
            result = payload if binary else (json.loads(payload) if payload else None)
            if return_headers:
                return response.status, result, response.headers
            return response.status, result

    def ok(self, path, body=None, **kwargs):
        status, result = self.request(path, body, **kwargs)
        check(200 <= status < 300, f'{path}: expected success, got {status}: {result}')
        return result

    def bad(self, path, body=None, expected=None, **kwargs):
        status, result = self.request(path, body, **kwargs)
        check(status == expected if expected else 400 <= status < 500,
              f'{path}: expected rejection, got {status}: {result}')
        check(isinstance(result, dict) and bool(result.get('message')), f'{path}: readable error missing')
        return result

    def upload(self, xml, name='events.xml', headers=None):
        boundary = 'GuessMarketTest' + uuid.uuid4().hex
        raw = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{name}"\r\n'
               'Content-Type: application/xml\r\n\r\n').encode() + xml + f'\r\n--{boundary}--\r\n'.encode()
        return self.request('/upload', method='POST', raw=raw,
                            content_type='multipart/form-data; boundary=' + boundary, headers=headers)

    def upload_state(self, saved, name='system.gmstate'):
        boundary = 'GuessMarketStateTest' + uuid.uuid4().hex
        raw = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{name}"\r\n'
               'Content-Type: application/octet-stream\r\n\r\n').encode() + saved + f'\r\n--{boundary}--\r\n'.encode()
        return self.request('/state', method='POST', raw=raw,
                            content_type='multipart/form-data; boundary=' + boundary)

    def download_state(self):
        return self.request('/state', headers={'Accept': 'application/octet-stream'}, binary=True)


def state(client):
    return client.ok('/snapshot')


def event_id(snapshot, text):
    return next(row['event']['id'] for row in snapshot['events'] if text in row['event']['name'])


def create_request(name, method='LMSR', **changes):
    request = {
        'name': name,
        'description': 'Created through the HTTP API',
        'commissionPercent': 4,
        'commissionType': 'ON_PURCHASE',
        'optionNames': ['Yes', 'No'],
        'tradingMethod': method,
    }
    if method == 'LMSR':
        request['liquidityParameter'] = 120
    else:
        request.update(initialInvestment=300, baseValue=10, mintAllowed=True)
    request.update(changes)
    return request


def main():
    anonymous = Client()
    health = anonymous.ok('/health')
    check(health == {'message': 'Guess Market is ready.', 'service': 'guess-market', 'apiVersion': 2},
          'health identity/version contract changed')

    origin = 'http://localhost:5173'
    status, cors_health, headers = anonymous.request('/health', headers={'Origin': origin}, return_headers=True)
    check(status == 200 and cors_health == health, 'approved web origin could not read health')
    check(headers.get('Access-Control-Allow-Origin') == origin, 'approved origin was not returned exactly')
    check('Origin' in headers.get('Vary', ''), 'CORS response is missing Vary: Origin')
    status, _, _ = anonymous.request('/health', headers={'Origin': origin, 'Host': 'rebind.example:8080'},
                                     return_headers=True)
    check(status == 403, 'approved origin bypassed the exact Host allowlist')
    status, _, _ = anonymous.request('/health', headers={'Origin': origin, 'Host': 'guess-market.test:8443'},
                                     return_headers=True)
    check(status == 200, 'explicitly configured browser Host was rejected')
    status, _, _ = anonymous.request('/health', headers={'Origin': origin, 'Host': 'localhost:49152'},
                                     return_headers=True)
    check(status == 200, 'browser Host validation incorrectly fixed the server port')
    status, payload, headers = anonymous.request('/snapshot', method='OPTIONS', headers={
        'Origin': origin,
        'Access-Control-Request-Method': 'GET',
        'Access-Control-Request-Headers': 'authorization,content-type',
        'Access-Control-Request-Private-Network': 'true',
    }, return_headers=True)
    check(status == 204 and payload is None, 'authenticated route preflight was not answered before auth')
    check(headers.get('Access-Control-Allow-Methods') == 'GET, POST, OPTIONS', 'preflight methods changed')
    check(headers.get('Access-Control-Allow-Headers') == 'Content-Type, Authorization', 'preflight headers changed')
    check(headers.get('Access-Control-Allow-Private-Network') == 'true', 'PNA preflight permission missing')
    status, error, headers = anonymous.request('/health', headers={'Origin': 'https://attacker.example'},
                                               return_headers=True)
    check(status == 403 and bool(error.get('message')), 'unapproved present Origin was not denied')
    check(headers.get('Access-Control-Allow-Origin') is None, 'unapproved origin received CORS permission')

    browser = Client()
    login = browser.ok('/login', {'username': 'Hosted Browser'}, headers={'Origin': origin})
    token = login.get('token')
    check(isinstance(token, str) and len(token) == 43, 'login did not return an opaque bearer token')
    check(len(base64.urlsafe_b64decode(token + '=')) == 32, 'browser token is not 256 bits')
    browser.jar.clear()
    bearer_headers = {'Origin': origin, 'Authorization': 'Bearer ' + token}
    check(browser.ok('/snapshot', headers=bearer_headers)['self']['name'] == 'Hosted Browser',
          'bearer identity did not survive without cookies')
    status, _ = Client().request('/snapshot?token=' + ('A' * 43))
    check(status == 401, 'token in a URL was accepted')
    browser.ok('/logout', {}, headers=bearer_headers)
    status, _ = browser.request('/snapshot', headers=bearer_headers)
    check(status == 401, 'logout did not revoke the bearer token')

    for path in ['/snapshot', '/quote?eventId=1&optionIndex=0&shares=1']:
        anonymous.bad(path, expected=401)
    anonymous.bad('/funds', {'amount': 10}, expected=401)
    anonymous.bad('/events/create', create_request('Anonymous event'), expected=401)
    anonymous.bad('/login', {'username': '   '})
    anonymous.bad('/login', {}, expected=400)
    anonymous.bad('/login', raw=b'{broken', method='POST')

    maker, trader = Client(), Client()
    maker.ok('/login', {'username': 'Market Maker'})
    Client().bad('/login', {'username': ' MARKET MAKER '}, expected=409)
    trader.ok('/login', {'username': 'Trader'})
    check(state(maker)['self']['balance'] == 0, 'new account must start empty')
    check(state(maker)['events'] == [], 'fresh server must have no events')
    for amount in [0, -1, 'NaN', 'Infinity', None, 'ten']:
        maker.bad('/funds', {'amount': amount})
    maker.ok('/funds', {'amount': 10000})
    trader.ok('/funds', {'amount': 1000})
    before = state(maker)
    for sample in ['small.xml', 'multiple.xml']:
        status, payload = maker.upload((ROOT / 'data' / 'ex3' / sample).read_bytes(), sample)
        check(200 <= status < 300, f'official sample {sample} rejected: {status}: {payload}')
    snapshot = state(trader)
    check(len(snapshot['events']) == 4, 'uploads must accumulate both supplied files')
    check(len({row['event']['id'] for row in snapshot['events']}) == 4, 'server event IDs must be unique')
    check(all(row['event']['marketMakerName'] == 'Market Maker' for row in snapshot['events']), 'uploader owns all events')
    check(snapshot['self']['name'] == 'Trader', 'private snapshot belongs to session')
    check(all(set(u) == {'name', 'balance', 'marketMaker'} for u in snapshot['users']), 'public users leak private data')
    small = (ROOT / 'data' / 'ex3' / 'small.xml').read_bytes()
    for xml in [small, small.replace(b'Mujtaba is Dead', b'mujtaba IS dead'),
                b'<!DOCTYPE Guess-Market [<!ENTITY x SYSTEM "file:///etc/passwd">]><Guess-Market>&x;</Guess-Market>',
                small.replace(b'<commission type="on-purchase">5', b'<commission type="on-purchase">91'),
                small.replace(b'<description>', b'<id>1</id><description>', 1)]:
        status, error = maker.upload(xml)
        check(400 <= status < 500 and bool(error.get('message')), f'invalid upload accepted: {status}: {error}')
        check(len(state(maker)['events']) == 4, 'rejected upload changed the market')
    # A valid first event followed by an existing one must roll back the whole upload.
    multi = (ROOT / 'data' / 'ex3' / 'multiple.xml').read_bytes()
    mixed = multi.replace(b'Earth Quake on Dead Sea', b'Fresh event before duplicate', 1)
    status, _ = maker.upload(mixed)
    check(400 <= status < 500 and len(state(maker)['events']) == 4, 'partial upload escaped validation')
    status, error = maker.upload(b'x' * (2 * 1024 * 1024 + 1))
    check(status == 413 and bool(error.get('message')), 'oversized upload was not rejected clearly')
    check(len(state(maker)['events']) == 4, 'oversized upload changed the market')
    maker.bad('/funds', raw=b'{\"amount\":1} trailing', method='POST')
    maker.bad('/funds', {'amount': 1}, method='GET', expected=405)
    maker.bad('/unknown-endpoint', expected=404)
    maker.bad('/funds', {'amount': 1}, content_type='text/plain', expected=415)
    status, _ = maker.upload(small, 'wrong.txt')
    check(400 <= status < 500, 'non-XML extension accepted')

    created_lmsr = maker.ok('/events/create', create_request('HTTP-created LMSR'))
    created_book = maker.ok('/events/create', create_request('HTTP-created order book', 'ORDER_BOOK',
        commissionType='ON_CLOSE'))
    check(created_lmsr['status'] == 'NOT_STARTED' and created_lmsr['tradingMethod'] == 'LMSR'
          and created_lmsr['lmsr']['liquidityParameter'] == 120,
          'HTTP-created LMSR settings changed')
    check(created_book['status'] == 'NOT_STARTED' and created_book['tradingMethod'] == 'ORDER_BOOK'
          and created_book['orderBook'] == {'initialInvestment': 300, 'initialPairs': 30,
                                            'baseValue': 10, 'mintAllowed': True},
          'HTTP-created order-book settings changed')
    created_ids = {created_lmsr['id'], created_book['id']}
    check(created_lmsr['marketMakerName'] == 'Market Maker'
          and created_book['marketMakerName'] == 'Market Maker',
          'event creator did not come from the authenticated session')
    maker_after_create = state(maker)
    check(created_ids.issubset(set(maker_after_create['self']['marketMakerEventIds']))
          and created_ids.issubset({row['event']['id'] for row in maker_after_create['involvements']}),
          'created events were not assigned to their market maker')
    trader_after_create = state(trader)
    visible = {row['event']['id']: row['event'] for row in trader_after_create['events']}
    check(created_ids.issubset(visible)
          and all(visible[event]['marketMakerName'] == 'Market Maker' for event in created_ids),
          'created events were not visible to another client')

    stable_create_state = (maker_after_create['events'], maker_after_create['self'],
                           maker_after_create['involvements'])
    invalid_creates = [
        create_request('Invalid liquidity', liquidityParameter=0),
        create_request('Duplicate outcomes', optionNames=['Same', ' same ']),
        dict(create_request('Forged owner'), userName='Trader'),
    ]
    for request in invalid_creates:
        maker.bad('/events/create', request)
    after_invalid_creates = state(maker)
    check((after_invalid_creates['events'], after_invalid_creates['self'],
           after_invalid_creates['involvements']) == stable_create_state,
          'rejected event creation changed events or market-maker ownership')

    lmsr = event_id(snapshot, 'Mujtaba')
    order_book = event_id(snapshot, 'Earth Quake')
    mint_book = event_id(snapshot, 'World Cap')

    web_uploader = Client()
    web_token = web_uploader.ok('/login', {'username': 'Browser Uploader'},
                                headers={'Origin': origin})['token']
    web_uploader.jar.clear()
    web_headers = {'Origin': origin, 'Authorization': 'Bearer ' + web_token}
    browser_xml = small.replace(b'Mujtaba is Dead', b'Browser bearer upload')
    status, uploaded = web_uploader.upload(browser_xml, headers=web_headers)
    check(200 <= status < 300 and uploaded['eventCount'] == 1,
          f'bearer-authenticated browser upload failed: {status}: {uploaded}')
    uploaded_id = event_id(web_uploader.ok('/snapshot', headers=web_headers), 'Browser bearer upload')
    uploaded_event = next(row['event'] for row in state(trader)['events']
                          if row['event']['id'] == uploaded_id)
    check(uploaded_event['marketMakerName'] == 'Browser Uploader',
          'bearer upload was not shared under its authenticated owner')

    trader.bad('/events/open', {'eventId': lmsr, 'userName': 'Market Maker'})
    maker.ok('/events/open', {'eventId': lmsr})
    quote = trader.ok(f'/quote?eventId={lmsr}&optionIndex=0&shares=10')
    expected_cost = 100 * (math.log(math.exp(.1) + 1) - math.log(2))
    check(abs(quote['cost'] - expected_cost) < 1e-8, 'LMSR quote regression')
    trader.ok('/events/buy', {'eventId': lmsr, 'optionIndex': 0, 'shares': 10})
    after_trade = state(trader)
    check(after_trade['self']['balance'] < 1000, 'purchase did not debit buyer')
    check(len(after_trade['accountEntries']) > 1, 'purchase ledger missing')
    check(len(state(maker)['accountEntries']) > len(before['accountEntries']), 'MM subsidy/commission ledger missing')
    trader.bad('/events/buy', {'eventId': lmsr, 'optionIndex': -1, 'shares': 1})
    trader.bad('/events/buy', {'eventId': lmsr, 'optionIndex': 0, 'shares': 0})
    trader.bad('/events/buy', {'eventId': lmsr, 'optionIndex': 0, 'shares': 1.5})
    trader.bad('/events/close', {'eventId': lmsr, 'winningOptionIndex': 0, 'userName': 'Market Maker'})
    maker.ok('/events/close', {'eventId': lmsr, 'winningOptionIndex': 0})
    check(state(trader)['self']['balance'] > after_trade['self']['balance'], 'settlement payout missing')
    trader.bad('/events/buy', {'eventId': lmsr, 'optionIndex': 0, 'shares': 1})

    maker.ok('/events/open', {'eventId': order_book})
    maker.ok('/events/order', {'eventId': order_book, 'optionIndex': 0, 'side': 'SELL', 'quantity': 10, 'pricePerShare': .5})
    trader.ok('/events/order', {'eventId': order_book, 'optionIndex': 0, 'side': 'BUY', 'quantity': 4, 'pricePerShare': .5})
    traded = next(e for e in state(trader)['events'] if e['event']['id'] == order_book)
    check(len(traded['tradeHistory']) >= 1, 'order book did not match')
    trader.bad('/events/order', {'eventId': order_book, 'optionIndex': 0, 'side': 'BUY', 'quantity': 1, 'pricePerShare': 1.01})
    maker.ok('/events/open', {'eventId': mint_book})
    maker.ok('/events/order', {'eventId': mint_book, 'optionIndex': 0, 'side': 'BUY', 'quantity': 5, 'pricePerShare': .5})
    trader.ok('/events/order', {'eventId': mint_book, 'optionIndex': 1, 'side': 'BUY', 'quantity': 5, 'pricePerShare': .5})
    minted = next(e for e in state(trader)['events'] if e['event']['id'] == mint_book)
    check(any(t['kind'] == 'MINT' for t in minted['tradeHistory']), 'opposite bids did not mint')

    # Cross-client delivery and sender identity must derive from the cookie, never JSON.
    trader.ok('/chat', {'message': 'Hello from Trader'})
    chat = state(maker)['chat']
    check(chat[-1]['username'] == 'Trader' and chat[-1]['message'] == 'Hello from Trader', 'chat not shared')
    trader.bad('/chat', {'message': ' '})
    trader.bad('/chat', {'message': 'x' * 5000})
    check(maker.ok('/snapshot?username=Trader')['self']['name'] == 'Market Maker', 'query parameter changed snapshot identity')

    # Exercise 3 state is separate from schema XML: it preserves server-generated IDs,
    # registered clients and operation history, while a restore replaces the shared market.
    saved_snapshot = state(maker)
    status, saved_state = maker.download_state()
    check(status == 200 and saved_state.startswith(b'GUESS-MARKET-EX3-STATE\n1\n'),
          'Exercise 3 state export is missing or unversioned')
    maker.ok('/funds', {'amount': 123})
    temporary = maker.ok('/events/create', create_request('Removed by state restore'))
    status, restored = maker.upload_state(saved_state)
    check(200 <= status < 300 and restored['eventCount'] == len(saved_snapshot['events'])
          and restored['userCount'] == len(saved_snapshot['users']),
          f'Exercise 3 state restore failed: {status}: {restored}')
    after_restore = state(maker)
    check(after_restore['events'] == saved_snapshot['events'],
          'state restore did not preserve generated event IDs and event history')
    check(after_restore['self'] == saved_snapshot['self']
          and after_restore['accountEntries'] == saved_snapshot['accountEntries'],
          'state restore did not preserve the importing account and operation history')
    check(all(row['event']['id'] != temporary['id'] for row in after_restore['events']),
          'state restore accumulated events instead of replacing the shared market')
    check(state(trader)['self']['name'] == 'Trader',
          'a connected user present in the restored state lost their account')
    check(after_restore['chat'] == [], 'chat from the replaced market was left behind')
    status, error = maker.upload_state((ROOT / 'data' / 'full-market-in-play.gmstate').read_bytes(),
                                       'exercise-two.gmstate')
    check(status == 400 and bool(error.get('message')), 'Exercise 2 state was accepted as Exercise 3 state')
    check(state(maker)['events'] == after_restore['events'], 'rejected state restore changed the market')
    if STATE_OUTPUT:
        STATE_OUTPUT.write_bytes(saved_state)

    # Concurrent servlet requests exercise synchronized registration and account changes.
    def race_login(_):
        return Client().request('/login', {'username': 'Race User'})[0]
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        statuses = list(pool.map(race_login, range(12)))
    check(sum(200 <= s < 300 for s in statuses) == 1, 'duplicate-name race admitted multiple sessions')
    start_balance = state(trader)['self']['balance']
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        responses = list(pool.map(lambda _: trader.request('/funds', {'amount': 1}), range(20)))
    check(all(200 <= s < 300 for s, _ in responses), 'concurrent deposits failed')
    check(abs(state(trader)['self']['balance'] - start_balance - 20) < 1e-8, 'concurrent deposits lost money')
    ledger = state(trader)['accountEntries']
    check(all(abs(e['balanceBefore'] + e['amount'] - e['balanceAfter']) < 1e-8 for e in ledger), 'ledger arithmetic inconsistent')
    check(len({e['sequence'] for e in ledger}) == len(ledger), 'ledger sequence is not unique')
    trader.ok('/logout', {})
    trader.bad('/snapshot', expected=401)
    Client().bad('/login', {'username': 'Trader'}, expected=409)
    print(f'PASS: {checks} HTTP checks against the deployed WAR')


if __name__ == '__main__':
    main()
