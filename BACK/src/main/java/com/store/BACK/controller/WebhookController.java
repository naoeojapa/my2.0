package com.store.BACK.controller;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import com.store.BACK.model.Pedido;
import com.store.BACK.model.PedidoAviso;
import com.store.BACK.repository.PedidoRepository;
import com.store.BACK.repository.PedidoAvisoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    @Value("${mercadopago.access_token}")
    private String accessToken;

    private final PedidoRepository pedidoRepository;
    private final PedidoAvisoRepository pedidoAvisoRepository;

    public WebhookController(PedidoRepository pedidoRepository, PedidoAvisoRepository pedidoAvisoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoAvisoRepository = pedidoAvisoRepository;
    }

    @PostMapping
    public ResponseEntity<?> receiveNotification(@RequestParam Map<String, String> params) {
        String topic = params.get("topic");
        String id = params.get("id");

        if (id == null) {
            return ResponseEntity.ok().build(); 
        }

        try {
            if ("payment".equals(topic) || params.containsKey("data.id")) {
                MercadoPagoConfig.setAccessToken(accessToken);
                PaymentClient client = new PaymentClient();
                
                Long paymentId = Long.parseLong(id);
                Payment payment = client.get(paymentId);

                if (payment != null && "approved".equals(payment.getStatus())) {
                    // CORREÇÃO: Passa o ID como Long direto, sem converter para String
                    Pedido pedido = pedidoRepository.findByPagamentoIdExterno(paymentId);
                    
                    // Fallback: Tenta achar pelo ID na descrição se não achar pelo ID externo
                    if (pedido == null && payment.getDescription() != null) {
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

                    if (pedido != null && !"PAGO".equals(pedido.getStatus())) {
                        // 1. Atualiza Status
                        pedido.setStatus("PAGO");
                        // Salva o ID externo caso não tenha
                        if (pedido.getPagamentoIdExterno() == null) {
                            pedido.setPagamentoIdExterno(paymentId);
                        }
                        pedidoRepository.save(pedido);

                        // 2. Cria aviso
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

    private void criarAvisoPagamento(Pedido pedido) {
        try {
            PedidoAviso aviso = new PedidoAviso();
            aviso.setPedido(pedido);
            // CORREÇÃO: PedidoAviso não tem setTitulo, removemos.
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
            String dataFormatada = LocalDateTime.now().format(formatter);
            
            // CORREÇÃO: Incluímos o "título" na mensagem
            String msg = "Pagamento Confirmado: O pagamento via PIX foi confirmado automaticamente pelo sistema em " + dataFormatada + ".";
            aviso.setMensagem(msg);
            
            // CORREÇÃO: O método correto é setDataAviso, não setDataCriacao
            aviso.setDataAviso(LocalDateTime.now());
            
            pedidoAvisoRepository.save(aviso);
        } catch (Exception e) {
            System.err.println("Erro ao criar aviso automático: " + e.getMessage());
        }
    }
}