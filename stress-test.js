import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

// Métricas customizadas
const okCount              = new Counter('transactions_ok');
const saldoInsuficiente    = new Counter('transactions_saldo_insuficiente');
const senhaInvalida        = new Counter('transactions_senha_invalida');
const cartaoInexistente    = new Counter('transactions_cartao_inexistente');
const unexpectedErrors     = new Counter('transactions_unexpected');

// Cartão compartilhado entre todas as VUs — cenário de concorrência real
const CARD_NUMBER = '7777888899990000';
const PASSWORD    = '4321';
const BASE_URL    = 'http://localhost:8080';
const AUTH        = 'dXNlcm5hbWU6cGFzc3dvcmQ='; // username:password em base64

export const options = {
    scenarios: {
        // Fase 1: setup — cria o cartão (1 VU, 1 iteração)
        setup_card: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 1,
            maxDuration: '10s',
            tags: { scenario: 'setup' },
        },
        // Fase 2: stress de concorrência — 50 VUs disparando transações simultâneas
        concurrent_transactions: {
            executor: 'constant-vus',
            vus: 50,
            duration: '30s',
            startTime: '5s', // aguarda setup terminar
            tags: { scenario: 'stress' },
        },
    },
    thresholds: {
        // Nenhuma transação pode retornar status inesperado (só 201 ou 422 são válidos)
        'transactions_unexpected': ['count == 0'],
        // p95 das requisições deve ser < 500ms
        'http_req_duration{scenario:stress}': ['p(95)<500'],
    },
};

const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Basic ${AUTH}`,
};

export function setup() {
    // Garante que o cartão existe com saldo R$10.00 para forçar race condition
    // Primeiro cria o cartão
    http.post(`${BASE_URL}/cartoes`, JSON.stringify({
        numeroCartao: CARD_NUMBER,
        senha: PASSWORD,
    }), { headers });

    // Drena até R$10.00 (500 - 490 = 10)
    http.post(`${BASE_URL}/transacoes`, JSON.stringify({
        numeroCartao: CARD_NUMBER,
        senhaCartao: PASSWORD,
        valor: 490.00,
    }), { headers });

    const res = http.get(`${BASE_URL}/cartoes/${CARD_NUMBER}`, { headers });
    console.log(`Saldo inicial para stress: R$${res.body}`);
}

export default function () {
    const scenario = __ENV.K6_SCENARIO || '';

    // Cada VU tenta debitar R$1.00 — com saldo de R$10.00 apenas 10 devem passar
    const res = http.post(`${BASE_URL}/transacoes`, JSON.stringify({
        numeroCartao: CARD_NUMBER,
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

export function teardown() {
    const res = http.get(`${BASE_URL}/cartoes/${CARD_NUMBER}`, { headers });
    console.log(`Saldo final apos stress: R$${res.body}`);
    check(res, {
        'saldo final >= 0': (r) => parseFloat(r.body) >= 0,
        'saldo final nao negativo (concorrencia ok)': (r) => parseFloat(r.body) >= 0.00,
    });
}
