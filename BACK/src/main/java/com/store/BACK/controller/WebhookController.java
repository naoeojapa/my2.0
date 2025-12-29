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
                    // Busca pelo ID externo salvo
                    Pedido pedido = pedidoRepository.findByPagamentoIdExterno(String.valueOf(paymentId));
                    
                    // Fallback: Tenta achar pelo ID na descrição
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

                    // CORREÇÃO AQUI: Usa string "PAGO" em vez de StatusPedido.PAGO
                    if (pedido != null && !"PAGO".equals(pedido.getStatus())) {
                        
                        // 1. Atualiza Status
                        pedido.setStatus("PAGO");
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
            aviso.setTitulo("Pagamento Confirmado");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
            String dataFormatada = LocalDateTime.now().format(formatter);
            
            aviso.setMensagem("O pagamento via PIX foi confirmado automaticamente pelo sistema em " + dataFormatada + ".");
            aviso.setDataCriacao(LocalDateTime.now());
            
            pedidoAvisoRepository.save(aviso);
        } catch (Exception e) {
            System.err.println("Erro ao criar aviso automático: " + e.getMessage());
        }
    }
}