document.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);
    const pedidoId = params.get('id');
    const token = localStorage.getItem('jwtToken');

    const qrCanvas = document.getElementById('qr-code-canvas');
    const pixInput = document.getElementById('pix-code-input');
    const valorTotalEl = document.getElementById('valor-total');
    
    // Elementos das telas
    const pendingBox = document.getElementById('payment-pending');
    const successBox = document.getElementById('payment-success');

    // Configuração de URL
    const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
    const BASE_URL = isLocal 
        ? 'http://localhost:8080/api'
        : 'https://back-production-e565.up.railway.app/api';

    // Verificações de Segurança
    if (!token) {
        window.location.href = '/FRONT/login/HTML/login.html';
        return;
    }

    if (!pedidoId) {
        alert('Pedido não encontrado.');
        window.location.href = '/index.html';
        return;
    }

    // Configuração Axios
    const apiClient = axios.create({
        baseURL: BASE_URL,
        headers: { 'Authorization': `Bearer ${token}` }
    });

    // 1. Carrega dados do pedido
    async function carregarPedido() {
        try {
            const res = await apiClient.get(`/pedidos/${pedidoId}`);
            const pedido = res.data;

            // Se já estiver pago, mostra tela de sucesso sem animação
            if (pedido.status === 'PAGO' || pedido.status === 'ENVIADO' || pedido.status === 'ENTREGUE') {
                mostrarSucesso(false); 
                return;
            }

            // Preenche valores
            valorTotalEl.textContent = `R$ ${pedido.valorTotal.toFixed(2).replace('.', ',')}`;
            
            // Gera QR Code
            if (pedido.pixCopiaECola) {
                renderizarQRCode(pedido.pixCopiaECola);
                pixInput.value = pedido.pixCopiaECola;
            }

            // Começa a vigiar o status
            iniciarMonitoramento();

        } catch (error) {
            console.error('Erro ao carregar pedido:', error);
            if (error.response && error.response.status === 403) {
                 window.location.href = '/FRONT/login/HTML/login.html';
            }
        }
    }

    // 3. Desenha o QR Code usando a biblioteca QRious
    function renderizarQRCode(textoPix) {
        if (!qrCanvas) return; 
        new QRious({
            element: qrCanvas,
            value: textoPix,
            size: 400, // Aumentado de 200 para 400 para garantir alta qualidade
            level: 'H' 
        });
    }

    window.copiarCodigo = function() {
        pixInput.select();
        pixInput.setSelectionRange(0, 99999);
        navigator.clipboard.writeText(pixInput.value).then(() => {
            const msg = document.getElementById('copy-msg');
            msg.style.opacity = '1';
            setTimeout(() => msg.style.opacity = '0', 2000);
        });
    }

    // 2. Monitoramento Automático (Polling)
    let intervalId = null;
    function iniciarMonitoramento() {
        if (intervalId) clearInterval(intervalId);
        
        intervalId = setInterval(async () => {
            try {
                const res = await apiClient.get(`/pedidos/${pedidoId}`);
                const status = res.data.status;
                
                // Se pagou, para de verificar e chama a animação
                if (status === 'PAGO' || status === 'ENVIADO') {
                    mostrarSucesso(true); // TRUE = Disparar animação!
                    clearInterval(intervalId);
                }
            } catch (error) {
                console.error('Erro no monitoramento:', error);
            }
        }, 5000); // Verifica a cada 5 segundos
    }

    // 3. Função de Sucesso e Animação
    function mostrarSucesso(animar = true) {
        if(pendingBox) pendingBox.classList.add('hidden');
        if(successBox) successBox.classList.remove('hidden');
        
        if (animar) {
            // Toca vibração no celular (tzzz tzzz)
            if (navigator.vibrate) navigator.vibrate([200, 100, 200, 100, 400]);
            
            // Dispara a chuva de confetes
            dispararConfetes();
        }
    }

    // Lógica da biblioteca Canvas Confetti
    function dispararConfetes() {
        var duration = 3 * 1000;
        var animationEnd = Date.now() + duration;
        var defaults = { startVelocity: 30, spread: 360, ticks: 60, zIndex: 9999 };

        function randomInRange(min, max) {
            return Math.random() * (max - min) + min;
        }

        var interval = setInterval(function() {
            var timeLeft = animationEnd - Date.now();

            if (timeLeft <= 0) {
                return clearInterval(interval);
            }

            var particleCount = 50 * (timeLeft / duration);
            
            // Lança confetes de dois lados da tela
            confetti(Object.assign({}, defaults, { 
                particleCount, 
                origin: { x: randomInRange(0.1, 0.3), y: Math.random() - 0.2 } 
            }));
            confetti(Object.assign({}, defaults, { 
                particleCount, 
                origin: { x: randomInRange(0.7, 0.9), y: Math.random() - 0.2 } 
            }));
        }, 250);
    }

    carregarPedido();
});