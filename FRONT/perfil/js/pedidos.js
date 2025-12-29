document.addEventListener('DOMContentLoaded', () => {
    const ordersContainer = document.getElementById('orders-container');
    const token = localStorage.getItem('jwtToken');
    
    // Elementos do DOM - Modais Antigos
    const detailsModal = document.getElementById('details-modal');
    const detailsModalBody = document.getElementById('details-modal-body');
    const updatesModal = document.getElementById('updates-modal');
    const updatesModalBody = document.getElementById('updates-modal-body');
    const imageLightboxModal = document.getElementById('image-lightbox-modal');
    const lightboxImage = document.getElementById('lightbox-image');
    
    // Elementos do DOM - Novo Modal de Confirmação
    const confirmModal = document.getElementById('confirmation-modal');
    const confirmBtn = document.getElementById('confirm-cancel-btn');
    const closeConfirmBtns = document.querySelectorAll('.close-confirm-btn'); // Botão "Não"
    const closeButtons = document.querySelectorAll('.close-modal-btn'); // Botões "X" dos outros modais

    let orderIdToCancel = null; // Variável para guardar qual ID vamos deletar

    const BASE_URL = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
        ? 'http://localhost:8080'
        : 'https://back-production-e565.up.railway.app';

    let currentOrders = [];

    // --- FUNÇÕES AUXILIARES ---
    const formatMessage = (text) => {
        if (!text) return '';
        return text.replace(/\n/g, '<br>');
    };

    const getImageUrl = (path) => {
        if (!path) return null;
        if (path.startsWith('http')) return path;
        const cleanPath = path.startsWith('/') ? path.substring(1) : path;
        return `${BASE_URL}/${cleanPath}`;
    };

    // --- FUNÇÃO ABRIR CONFIRMAÇÃO DE CANCELAMENTO ---
    const askCancelOrder = (id) => {
        orderIdToCancel = id;
        confirmModal.classList.add('active');
    };

    // --- FUNÇÃO EXECUTAR CANCELAMENTO ---
    const executeCancellation = async () => {
        if (!orderIdToCancel) return;

        // Botão visual carregando
        const originalText = confirmBtn.innerText;
        confirmBtn.innerText = 'Cancelando...';
        confirmBtn.disabled = true;

        try {
            await axios.delete(`${BASE_URL}/api/pedidos/${orderIdToCancel}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            
            // Fecha modal
            confirmModal.classList.remove('active');

            // Feedback Visual no Card
            const card = document.querySelector(`.order-card[data-order-id="${orderIdToCancel}"]`);
            if(card) {
                card.innerHTML = `
                    <div class="deleted-msg" style="padding:40px; color:#ff4444;">
                        <i class="fas fa-check-circle" style="font-size:2rem; margin-bottom:10px;"></i><br>
                        Pedido Cancelado com Sucesso
                    </div>
                `;
                setTimeout(() => {
                    fetchOrders(); // Recarrega a lista
                }, 1500);
            } else {
                fetchOrders();
            }

        } catch (error) {
            console.error(error);
            // Fecha o modal de confirmação mesmo com erro, para mostrar o alert se necessário
            confirmModal.classList.remove('active');
            alert('Erro ao cancelar o pedido. Tente novamente.');
        } finally {
            // Restaura botão e reseta variável
            confirmBtn.innerText = originalText;
            confirmBtn.disabled = false;
            orderIdToCancel = null;
        }
    };

    // --- ABRIR MODAL DE DETALHES ---
    const openDetailsModal = (orderId) => {
        const order = currentOrders.find(o => o.id == orderId);
        if (!order) return;

        const formattedDate = new Date(order.dataPedido).toLocaleString('pt-BR');
        
        let enderecoCompleto = 'Endereço não informado';
        if (order.enderecoDeEntrega) {
            const end = order.enderecoDeEntrega;
            enderecoCompleto = `${end.rua}, ${end.numero}${end.complemento ? ` - ${end.complemento}` : ''}<br>
                                ${end.bairro} - ${end.cidade}/${end.estado}<br>
                                CEP: ${end.cep}`;
        }

        const temCaixa = order.comCaixa 
            ? '<span style="color:var(--primary); font-weight:bold;">Sim (+5%)</span>' 
            : 'Não (Padrão)';
        
        const temPrioridade = order.entregaPrioritaria 
            ? '<span style="color:var(--primary); font-weight:bold;">Sim (+5%)</span>' 
            : 'Não (Padrão)';

        detailsModalBody.innerHTML = `
            <div class="modal-section">
                <h4>Resumo do Pedido</h4>
                <p><strong>ID do Pedido:</strong> #${order.id}</p>
                <p><strong>Data:</strong> ${formattedDate}</p>
                <p><strong>Status:</strong> <span class="order-status-modal ${order.status ? order.status.toLowerCase() : ''}">${order.status}</span></p>
                <p><strong>Total:</strong> ${order.valorTotal.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}</p>
            </div>

            <div class="modal-section" style="border: 1px dashed var(--border-color);">
                <h4><i class="fas fa-truck-loading"></i> Preferências de Envio</h4>
                <p><strong>Embalagem com Caixa Original:</strong> ${temCaixa}</p>
                <p><strong>Entrega Prioritária:</strong> ${temPrioridade}</p>
            </div>

            <div class="modal-section">
                <h4>Endereço de Entrega</h4>
                <p><strong>Destinatário:</strong> ${order.nomeDestinatario || 'Não informado'}</p>
                <p><strong>Endereço:</strong><br>${enderecoCompleto}</p>
                ${order.observacoes ? `<p><strong>Observações:</strong> ${order.observacoes}</p>` : ''}
            </div>

            <div class="modal-section">
                <h4>Itens do Pedido</h4>
                ${order.itens.map(item => {
                    const imagePath = item.produto && item.produto.imagemUrl ? getImageUrl(item.produto.imagemUrl) : null;
                    return `
                    <div class="order-item-modal">
                        ${imagePath 
                            ? `<img src="${imagePath}" alt="${item.produto.nome}" class="order-item-image-modal">` 
                            : `<div style="width:80px;height:80px;background:#eee;"></div>`}
                        
                        <div class="order-item-details-modal">
                            <h5>${item.produto.nome}</h5>
                            <p>Tamanho: ${item.tamanho || 'N/A'}</p>
                            <p>Quantidade: ${item.quantidade}</p>
                            <p>Valor Unit.: ${item.precoUnitario.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}</p>
                        </div>
                    </div>
                `}).join('')}
            </div>
        `;
        detailsModal.classList.add('active');
    };

    // --- ABRIR MODAL DE ATUALIZAÇÕES ---
    const openUpdatesModal = async (orderId) => {
        try {
            updatesModalBody.innerHTML = '<p>Carregando atualizações...</p>';
            updatesModal.classList.add('active');

            const response = await axios.get(`${BASE_URL}/api/pedidos/${orderId}/avisos`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            const avisos = response.data;

            if (avisos.length === 0) {
                updatesModalBody.innerHTML = '<p>Nenhuma atualização para este pedido.</p>';
            } else {
                updatesModalBody.innerHTML = avisos.map(aviso => `
                    <div class="update-item">
                        <p><strong>${new Date(aviso.dataAviso).toLocaleString('pt-BR')}</strong></p>
                        <p>${formatMessage(aviso.mensagem)}</p>
                        ${aviso.imagemUrl ? `<img src="${getImageUrl(aviso.imagemUrl)}" alt="Imagem do aviso" class="update-image">` : ''}
                    </div>
                `).join('');
            }

            await axios.post(`${BASE_URL}/api/pedidos/${orderId}/avisos/mark-as-read`, {}, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            const orderCard = ordersContainer.querySelector(`.order-card[data-order-id='${orderId}']`);
            if (orderCard) {
                const badge = orderCard.querySelector('.notification-badge');
                if (badge) badge.classList.add('hidden');
            }

        } catch (error) {
            console.error('Erro ao buscar atualizações:', error);
            updatesModalBody.innerHTML = '<p>Erro ao carregar atualizações.</p>';
        }
    };

    // --- CHECAR AVISOS NÃO LIDOS ---
    const checkUnreadAvisos = async (orderId) => {
        try {
            const response = await axios.get(`${BASE_URL}/api/pedidos/${orderId}/avisos`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            const hasUnread = response.data.some(aviso => !aviso.lido);
            
            if (hasUnread) {
                const badge = document.querySelector(`.order-card[data-order-id='${orderId}'] .notification-badge`);
                if (badge) badge.classList.remove('hidden');
            }
        } catch (e) { /* Silêncio */ }
    };

    // --- BUSCAR PEDIDOS ---
    const fetchOrders = async () => {
        if (!token) {
            ordersContainer.innerHTML = '<p>Você precisa estar logado.</p>';
            return;
        }
        ordersContainer.innerHTML = '<p>Carregando pedidos...</p>';

        try {
            const response = await axios.get(`${BASE_URL}/api/pedidos`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            const sorted = response.data.sort((a,b) => new Date(b.dataPedido) - new Date(a.dataPedido));
            renderOrders(sorted);
        } catch (error) {
            console.error('Erro ao buscar pedidos:', error);
            ordersContainer.innerHTML = '<p>Não foi possível carregar seus pedidos.</p>';
        }
    };

    // --- RENDERIZAR PEDIDOS ---
    const renderOrders = async (orders) => {
        if (!orders || orders.length === 0) {
            ordersContainer.innerHTML = '<p>Você ainda não fez nenhum pedido.</p>';
            return;
        }

        currentOrders = orders;

        ordersContainer.innerHTML = orders.map(order => {
            const statusClass = order.status ? order.status.toLowerCase() : '';
            const formattedDate = new Date(order.dataPedido).toLocaleDateString('pt-BR');
            const formattedTotal = order.valorTotal.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

            let actionButtons = '';
            if (order.status === 'PENDENTE') {
                actionButtons = `
                    <div class="pending-actions">
                        <a href="../../pagamento/HTML/pagamento.html?id=${order.id}" class="btn-pay-now">
                            <i class="fas fa-qrcode"></i> Pagar Agora
                        </a>
                        <button class="btn-cancel-order" data-id="${order.id}">
                            <i class="fas fa-trash"></i> Cancelar
                        </button>
                    </div>
                `;
            }

            return `
            <div class="order-card" data-order-id="${order.id}">
                <div class="order-header">
                    <span class="order-id">Pedido #${order.id}</span>
                    <span class="order-date">Data: ${formattedDate}</span>
                    <span class="order-status ${statusClass}">${order.status}</span>
                </div>
                <div class="order-details-summary">
                    <strong>Total: ${formattedTotal}</strong>
                </div>
                <div class="order-body">
                    ${order.itens ? order.itens.map(item => {
                        const imgUrl = item.produto && item.produto.imagemUrl ? getImageUrl(item.produto.imagemUrl) : null;
                        return `
                        <div class="order-item">
                            ${imgUrl 
                                ? `<img src="${imgUrl}" class="order-item-image">` 
                                : `<div class="order-item-placeholder">SEM FOTO</div>`}
                            <div class="order-item-details">
                                <h4>${item.produto.nome}</h4>
                                <p>Tamanho: ${item.tamanho || 'N/A'}</p>
                                <p>Qtd: ${item.quantidade}</p>
                            </div>
                        </div>`;
                    }).join('') : '<p>Itens indisponíveis.</p>'}
                </div>
                <div class="order-footer">
                    <button class="btn btn-secondary view-details-btn">Ver Detalhes</button>
                    <button class="btn btn-primary view-updates-btn">
                        Ver Atualizações
                        <span class="notification-badge hidden"></span>
                    </button>
                </div>
                ${actionButtons}
            </div>
        `}).join('');

        orders.forEach(order => checkUnreadAvisos(order.id));
    };

    // --- EVENT LISTENERS ---

    // Listener para o botão de confirmação "Sim, Cancelar"
    confirmBtn.addEventListener('click', executeCancellation);

    // Listener para os botões "Não" do modal de confirmação
    closeConfirmBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            confirmModal.classList.remove('active');
            orderIdToCancel = null;
        });
    });

    // Fechar modal de confirmação clicando fora
    confirmModal.addEventListener('click', (e) => {
        if (e.target === confirmModal) {
            confirmModal.classList.remove('active');
            orderIdToCancel = null;
        }
    });

    // Listeners principais (Delegação de Eventos)
    ordersContainer.addEventListener('click', (event) => {
        const target = event.target;
        
        // Clique no botão cancelar -> Abre o Modal
        if (target.classList.contains('btn-cancel-order') || target.closest('.btn-cancel-order')) {
            const btn = target.closest('.btn-cancel-order');
            const id = btn.dataset.id;
            // Chama a nova função de abrir modal
            askCancelOrder(id); 
            return;
        }

        const orderCard = target.closest('.order-card');
        if (!orderCard) return;
        
        const orderId = orderCard.dataset.orderId;

        if (target.classList.contains('view-details-btn')) {
            openDetailsModal(orderId);
        }
        if (target.classList.contains('view-updates-btn') || target.closest('.view-updates-btn')) {
            openUpdatesModal(orderId);
        }
    });

    // Fechar Modais Gerais
    closeButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            detailsModal.classList.remove('active');
            updatesModal.classList.remove('active');
            imageLightboxModal.classList.remove('active');
        });
    });

    // Lightbox
    updatesModalBody.addEventListener('click', (e) => {
        if (e.target.classList.contains('update-image')) {
            lightboxImage.src = e.target.src;
            imageLightboxModal.classList.add('active');
        }
    });

    fetchOrders();
});