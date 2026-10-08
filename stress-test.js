import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

http.setResponseCallback(http.expectedStatuses(200, 201, 422));

// Métricas customizadas
const okCount              = new Counter('transactions_ok');
const saldoInsuficiente    = new Counter('transactions_saldo_insuficiente');
const senhaInvalida        = new Counter('transactions_senha_invalida');
const cartaoInexistente    = new Counter('transactions_cartao_inexistente');
const unexpectedErrors     = new Counter('transactions_unexpected');

const PASSWORD    = '4321';
const BASE_URL    = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH        = 'dXNlcm5hbWU6cGFzc3dvcmQ='; // username:password em base64

export const options = {
    scenarios: {
        // 50 VUs exercise concurrent debits against the same card.
        concurrent_transactions: {
            executor: 'constant-vus',
            vus: 50,
            duration: '30s',
        },
    },
    thresholds: {
        // Nenhuma transação pode retornar status inesperado (só 201 ou 422 são válidos)
        'transactions_unexpected': ['count == 0'],
        // p95 das requisições deve ser < 500ms
        'http_req_duration{scenario:concurrent_transactions}': ['p(95)<500'],
    },
};

const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Basic ${AUTH}`,
};

export function setup() {
    const cardNumber = `7${Date.now().toString().slice(-8)}${Math.floor(Math.random() * 1e7).toString().padStart(7, '0')}`;
    const createResponse = http.post(`${BASE_URL}/cartoes`, JSON.stringify({
        numeroCartao: cardNumber,
        senha: PASSWORD,
    }), { headers });
    if (!check(createResponse, { 'cartao de teste criado': (res) => res.status === 201 })) {
        throw new Error(`Falha ao criar cartão para stress test: status=${createResponse.status} body=${createResponse.body}`);
    }

    const debitResponse = http.post(`${BASE_URL}/transacoes`, JSON.stringify({
        numeroCartao: cardNumber,
        senhaCartao: PASSWORD,
        valor: 490.00,
    }), { headers });
    if (!check(debitResponse, { 'saldo de teste preparado': (res) => res.status === 201 && res.body === 'OK' })) {
        throw new Error(`Falha ao preparar saldo para stress test: status=${debitResponse.status} body=${debitResponse.body}`);
    }

    const res = http.get(`${BASE_URL}/cartoes/${cardNumber}`, { headers });
    if (!check(res, { 'saldo inicial e R$10.00': (response) => response.status === 200 && Number(response.body) === 10 })) {
        throw new Error(`Saldo inicial inesperado para stress test: status=${res.status} body=${res.body}`);
    }
    console.log(`Saldo inicial para stress: R$${res.body}`);
    return { cardNumber };
}

export default function (data) {
    // Cada VU tenta debitar R$1.00 — com saldo de R$10.00 apenas 10 devem passar
    const res = http.post(`${BASE_URL}/transacoes`, JSON.stringify({
        numeroCartao: data.cardNumber,
        senhaCartao: PASSWORD,
        valor: 1.00,
    }), { headers });

    if (res.status === 201 && res.body === 'OK') {
        okCount.add(1);
        check(res, { 'transacao autorizada - body OK': (r) => r.body === 'OK' });
    } else if (res.status === 422 && res.body === 'SALDO_INSUFICIENTE') {
        saldoInsuficiente.add(1);
        check(res, { 'saldo insuficiente - 422': (r) => r.status === 422 });
    } else if (res.status === 422 && res.body === 'SENHA_INVALIDA') {
        senhaInvalida.add(1);
    } else if (res.status === 422 && res.body === 'CARTAO_INEXISTENTE') {
        cartaoInexistente.add(1);
    } else {
        unexpectedErrors.add(1);
        console.error(`Resposta inesperada: status=${res.status} body=${res.body}`);
    }

    sleep(0.1);
}

export function teardown(data) {
    const res = http.get(`${BASE_URL}/cartoes/${data.cardNumber}`, { headers });
    console.log(`Saldo final apos stress: R$${res.body}`);
    check(res, {
        'saldo final >= 0': (r) => parseFloat(r.body) >= 0,
        'saldo final nao negativo (concorrencia ok)': (r) => parseFloat(r.body) >= 0.00,
    });
}
