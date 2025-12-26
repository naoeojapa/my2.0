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

    // 1. Configuração da URL da API
    // Verifica se está rodando no seu computador (localhost) ou na internet (Railway)
    const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
    
    const BASE_URL = isLocal 
        ? 'http://localhost:8080' 
        : 'https://back-production-e565.up.railway.app';

    // Verifica login
    if (!token) {
        window.location.href = '../../login/HTML/login.html';
        return;
    }

    // Verifica ID do pedido
    if (!pedidoId) {
        alert('Pedido não encontrado.');
        window.location.href = '../../index.html';
        return;
    }

    // Configuração do Axios
    // Adicionamos "/api" aqui para bater certo com o seu Java (@RequestMapping("/api/pedidos"))
    const apiClient = axios.create({
        baseURL: `${BASE_URL}/api`,
        headers: { 'Authorization': `Bearer ${token}` }
    });

    // 2. Carrega os dados do pedido ao abrir a tela
    async function carregarPedido() {
        try {
            // Vai chamar: .../api/pedidos/{id}
            const res = await apiClient.get(`/pedidos/${pedidoId}`);
            const pedido = res.data;

            // Se já estiver pago, mostra sucesso direto
            if (pedido.status === 'PAGO' || pedido.status === 'ENVIADO' || pedido.status === 'ENTREGUE') {
                mostrarSucesso();
                return;
            }

            // Preenche valores na tela
            valorTotalEl.textContent = `R$ ${pedido.valorTotal.toFixed(2).replace('.', ',')}`;
            
            // Se tiver o código Pix, desenha
            if (pedido.pixCopiaECola) {
                renderizarQRCode(pedido.pixCopiaECola);
                pixInput.value = pedido.pixCopiaECola;
            }

            // Inicia a verificação automática (Polling)
            iniciarMonitoramento();

        } catch (error) {
            console.error('Erro ao carregar pedido:', error);
            alert('Erro ao carregar detalhes do pagamento. Verifique o console.');
        }
    }

    // 3. Desenha o QR Code usando a biblioteca QRious
    function renderizarQRCode(textoPix) {
        if (!qrCanvas) return; // Proteção caso o canvas não exista
        new QRious({
            element: qrCanvas,
            value: textoPix,
            size: 400,
            level: 'H' // Alta correção de erro
        });
    }

    // 4. Função do botão Copiar
    window.copiarCodigo = function() {
        if (!pixInput.value) return;
        
        pixInput.select();
        pixInput.setSelectionRange(0, 99999); // Mobile
        
        navigator.clipboard.writeText(pixInput.value)
            .then(() => {
                const msg = document.getElementById('copy-msg');
                if (msg) {
                    msg.style.opacity = '1';
                    setTimeout(() => msg.style.opacity = '0', 2000);
                }
            })
            .catch(err => console.error('Erro ao copiar:', err));
    }

    // 5. Polling: Verifica o status a cada 5 segundos
    let intervalId = null;
    function iniciarMonitoramento() {
        // Limpa intervalo anterior se existir
        if (intervalId) clearInterval(intervalId);

        intervalId = setInterval(async () => {
            try {
                const res = await apiClient.get(`/pedidos/${pedidoId}`);
                const status = res.data.status;
                console.log('Verificando status...', status);

                if (status === 'PAGO' || status === 'ENVIADO') {
                    mostrarSucesso();
                    clearInterval(intervalId); // Para de verificar
                }
            } catch (error) {
                console.error('Erro no monitoramento:', error);
                // Não paramos o intervalo em caso de erro de rede temporário
            }
        }, 5000);
    }

    function mostrarSucesso() {
        if (pendingBox) pendingBox.classList.add('hidden');
        if (successBox) successBox.classList.remove('hidden');
        
        // Toca um som ou vibra (opcional para mobile)
        if (navigator.vibrate) navigator.vibrate([200, 100, 200]);
    }

    // Iniciar
    carregarPedido();
});