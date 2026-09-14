#!/usr/bin/env python3
"""Black-box Exercise 3 checks against a fresh deployed WAR (standard library only)."""
import concurrent.futures
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

    def request(self, path, body=None, method=None, raw=None, content_type=None):
        data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
        request = urllib.request.Request(BASE + path, data=data, method=method,
            headers={'Content-Type': content_type or 'application/json'} if data is not None else {})
        try:
            response = self.opener.open(request, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            payload = response.read()
            return response.status, json.loads(payload) if payload else None

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

    def upload(self, xml, name='events.xml'):
        boundary = 'GuessMarketTest' + uuid.uuid4().hex
        raw = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{name}"\r\n'
               'Content-Type: application/xml\r\n\r\n').encode() + xml + f'\r\n--{boundary}--\r\n'.encode()
        return self.request('/upload', method='POST', raw=raw, content_type='multipart/form-data; boundary=' + boundary)


def state(client):
    return client.ok('/snapshot')


def event_id(snapshot, text):
    return next(row['event']['id'] for row in snapshot['events'] if text in row['event']['name'])


def main():
    anonymous = Client()
    anonymous.ok('/health')
    for path in ['/snapshot', '/quote?eventId=1&optionIndex=0&shares=1']:
        anonymous.bad(path, expected=401)
    anonymous.bad('/funds', {'amount': 10}, expected=401)
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
    status, _ = maker.upload(small, 'wrong.txt')
    check(400 <= status < 500, 'non-XML extension accepted')

    lmsr = event_id(snapshot, 'Mujtaba')
    order_book = event_id(snapshot, 'Earth Quake')
    mint_book = event_id(snapshot, 'World Cap')
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
