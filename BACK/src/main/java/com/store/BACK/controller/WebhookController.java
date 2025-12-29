package com.store.BACK.controller;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import com.store.BACK.model.Pedido;
import com.store.BACK.model.PedidoAviso; // IMPORTANTE
import com.store.BACK.model.StatusPedido;
import com.store.BACK.repository.PedidoRepository;
import com.store.BACK.repository.PedidoAvisoRepository; // IMPORTANTE
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // Para formatar a hora bonitinha
import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    @Value("${mercadopago.access_token}")
    private String accessToken;

    private final PedidoRepository pedidoRepository;
    private final PedidoAvisoRepository pedidoAvisoRepository; // Repositório de Avisos

    // Construtor atualizado recebendo o PedidoAvisoRepository
    public WebhookController(PedidoRepository pedidoRepository, PedidoAvisoRepository pedidoAvisoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoAvisoRepository = pedidoAvisoRepository;
    }

    @PostMapping
    public ResponseEntity<?> receiveNotification(@RequestParam Map<String, String> params) {
        String topic = params.get("topic"); // ou "type" dependendo da versão da API
        String id = params.get("id"); // ou "data.id"

        if (id == null) {
            // Tenta pegar do corpo se não vier na URL (alguns webhooks mandam JSON)
            return ResponseEntity.ok().build(); 
        }

        try {
            if ("payment".equals(topic) || params.containsKey("data.id")) {
                MercadoPagoConfig.setAccessToken(accessToken);
                PaymentClient client = new PaymentClient();
                
                Long paymentId = Long.parseLong(id);
                Payment payment = client.get(paymentId);

                if (payment != null && "approved".equals(payment.getStatus())) {
                    // O pagamento foi aprovado! Vamos achar o pedido.
                    String externalId = payment.getExternalReference(); // Geralmente usamos isso ou buscamos pelo ID do pagamento salvo
                    
                    // Como fallback, buscamos pelo ID do pagamento se você salvou antes
                    Pedido pedido = pedidoRepository.findByPagamentoIdExterno(String.valueOf(paymentId));
                    
                    if (pedido == null && payment.getDescription() != null) {
                        // Tenta extrair o ID do pedido da descrição "Pedido #123..."
                        try {
                            String desc = payment.getDescription();
                            if(desc.contains("#")) {
                                String idStr = desc.split("#")[1].split(" ")[0];
                                Long pedidoId = Long.parseLong(idStr);
                                pedido = pedidoRepository.findById(pedidoId).orElse(null);
                            }
                        } catch (Exception e) {
                            System.err.println("Erro ao extrair ID do pedido: " + e.getMessage());
                        }
                    }

                    if (pedido != null && pedido.getStatus() != StatusPedido.PAGO) {
                        // 1. Atualiza Status
                        pedido.setStatus(StatusPedido.PAGO);
                        pedidoRepository.save(pedido);

                        // 2. CRIA A ATUALIZAÇÃO AUTOMÁTICA (AQUI ESTÁ A MÁGICA)
                        criarAvisoPagamento(pedido);
                        
                        System.out.println("Pedido #" + pedido.getId() + " atualizado para PAGO.");
                    }
                }
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // Função auxiliar para gerar a mensagem bonita
    private void criarAvisoPagamento(Pedido pedido) {
        try {
            PedidoAviso aviso = new PedidoAviso();
            aviso.setPedido(pedido);
            aviso.setTitulo("Pagamento Confirmado");
            
            // Formata a data: "28/12/2025 às 14:30"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
            String dataFormatada = LocalDateTime.now().format(formatter);
            
            aviso.setMensagem("O pagamento via PIX foi confirmado automaticamente pelo sistema em " + dataFormatada + ".");
            aviso.setDataCriacao(LocalDateTime.now());
            
            // Define quem enviou (pode ser null ou um usuário "Sistema")
            // aviso.setUsuario(null); // Sistema
            
            pedidoAvisoRepository.save(aviso);
        } catch (Exception e) {
            System.err.println("Erro ao criar aviso automático: " + e.getMessage());
        }
    }
}