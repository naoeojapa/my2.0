package com.store.BACK.service;

import com.store.BACK.model.ItemPedido;
import com.store.BACK.model.Pedido;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {

    // ✅ Injeta o disparador oficial do Spring (configurado via application.properties)
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String REMETENTE_GMAIL;

    // Cores da marca
    private final String COLOR_PRIMARY = "#ff7a00";
    private final String COLOR_PRIMARY_LIGHT = "#ff9a3d";
    private final String COLOR_BG = "#f8f9fa";
    private final String COLOR_CARD = "#ffffff";
    private final String COLOR_TEXT = "#2d3748";
    private final String COLOR_TEXT_LIGHT = "#718096";
    private final String COLOR_BORDER = "#e2e8f0";
    private final String COLOR_SUCCESS = "#48bb78";
    private final String COLOR_WARNING = "#ed8936";
    private final String COLOR_ERROR = "#f56565";
    private final String COLOR_INFO = "#3498db";

    public void enviarConfirmacaoPagamento(Pedido pedido) {
        enviarPedidoRecebido(pedido);
    }

    // --- MÉTODO DE ENVIO CENTRALIZADO VIA GMAIL ---
    private void dispararEmail(String destinatario, String assunto, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(REMETENTE_GMAIL, "Japa Universe");
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erro ao enviar e-mail via Gmail: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 1. PEDIDO RECEBIDO
    @Async
    @Transactional
    public void enviarPedidoRecebido(Pedido pedido) {
        String bodyContent =
                "<div style='text-align: center; margin-bottom: 30px;'>" +
                        "<div style='background-color: " + COLOR_WARNING + "; width: 60px; height: 60px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 15px;'>" +
                        "<span style='color: white; font-size: 24px;'>🕒</span>" +
                        "</div>" +
                        "<h1 style='color: " + COLOR_TEXT + "; margin: 0 0 10px 0; font-size: 28px;'>Pedido Recebido!</h1>" +
                        "<p style='color: " + COLOR_TEXT_LIGHT + "; margin: 0; font-size: 16px;'>Obrigado pela sua compra, " + pedido.getUsuario().getNome() + "!</p>" +
                        "</div>" +
                        buildItensHtml(pedido) +
                        buildEnderecoHtml(pedido) +
                        buildSuporteFooter();

        dispararEmail(pedido.getUsuario().getEmail(), 
                     "⏳ Pedido Recebido - Japa Universe #" + pedido.getId(), 
                     getBaseTemplate(bodyContent, "Pedido Recebido"));
    }

    // 2. PAGAMENTO CONFIRMADO
    @Async
    @Transactional
    public void enviarPagamentoConfirmado(Pedido pedido) {
        String bodyContent =
                "<div style='text-align: center; margin-bottom: 30px;'>" +
                        "<div style='background-color: " + COLOR_SUCCESS + "; width: 60px; height: 60px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 15px;'>" +
                        "<span style='color: white; font-size: 24px;'>✓</span>" +
                        "</div>" +
                        "<h1 style='color: " + COLOR_TEXT + "; margin: 0 0 10px 0; font-size: 28px;'>Pagamento Confirmado!</h1>" +
                        "<p style='color: " + COLOR_TEXT_LIGHT + "; margin: 0; font-size: 16px;'>Pagamento confirmado, " + pedido.getUsuario().getNome() + "!</p>" +
                        "</div>" +
                        buildItensHtml(pedido) +
                        buildEnderecoHtml(pedido) +
                        buildSuporteFooter();

        dispararEmail(pedido.getUsuario().getEmail(), 
                     "✅ Pagamento Confirmado - Japa Universe #" + pedido.getId(), 
                     getBaseTemplate(bodyContent, "Pagamento Confirmado"));
    }

    // 3. PEDIDO ENVIADO
    @Async
    @Transactional
    public void enviarPedidoEnviado(Pedido pedido) {
        String rastreioHtml = "";
        if (pedido.getCodigoRastreio() != null && !pedido.getCodigoRastreio().isEmpty()) {
             rastreioHtml = "<div style='background: #e3f2fd; border: 1px solid " + COLOR_INFO + "; border-radius: 8px; padding: 20px; margin: 25px 0; text-align: center;'>" +
                    "<h3 style='margin: 0 0 10px 0; color: " + COLOR_INFO + ";'>Código de Rastreio</h3>" +
                    "<p><strong>" + pedido.getCodigoRastreio() + "</strong></p>" +
                    "<a href='" + pedido.getLinkRastreio() + "' style='background-color: " + COLOR_INFO + "; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>RASTREAR</a>" +
                    "</div>";
        }

        String bodyContent =
                "<div style='text-align: center; margin-bottom: 30px;'>" +
                        "<div style='background-color: " + COLOR_INFO + "; width: 60px; height: 60px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 15px;'>" +
                        "<span style='color: white; font-size: 24px;'>🚚</span>" +
                        "</div>" +
                        "<h1 style='color: " + COLOR_TEXT + "; margin: 0 0 10px 0; font-size: 28px;'>Pedido Enviado!</h1>" +
                        "<p style='color: " + COLOR_TEXT_LIGHT + "; margin: 0; font-size: 16px;'>Seu pedido está a caminho!</p>" +
                        "</div>" +
                        rastreioHtml +
                        buildItensHtml(pedido) +
                        buildEnderecoHtml(pedido) +
                        buildSuporteFooter();

        dispararEmail(pedido.getUsuario().getEmail(), 
                     "🚚 Seu Pedido Foi Enviado - Japa Universe #" + pedido.getId(), 
                     getBaseTemplate(bodyContent, "Pedido Enviado"));
    }

    // 4. PEDIDO ENTREGUE
    @Async
    @Transactional
    public void enviarPedidoEntregue(Pedido pedido) {
        String bodyContent =
                "<div style='text-align: center; margin-bottom: 30px;'>" +
                        "<div style='background-color: " + COLOR_SUCCESS + "; width: 60px; height: 60px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 15px;'>" +
                        "<span style='color: white; font-size: 24px;'>🎁</span>" +
                        "</div>" +
                        "<h1 style='color: " + COLOR_TEXT + "; margin: 0 0 10px 0; font-size: 28px;'>Pedido Entregue!</h1>" +
                        "</div>" +
                        buildItensHtml(pedido) +
                        buildEnderecoHtml(pedido) +
                        buildSuporteFooter();

        dispararEmail(pedido.getUsuario().getEmail(), 
                     "🎁 Seu Pedido Foi Entregue - Japa Universe #" + pedido.getId(), 
                     getBaseTemplate(bodyContent, "Pedido Entregue"));
    }

    // 5. PEDIDO CANCELADO
    @Async
    @Transactional
    public void enviarPedidoCancelado(Pedido pedido) {
        String bodyContent =
                "<div style='text-align: center; margin-bottom: 30px;'>" +
                        "<div style='background-color: " + COLOR_ERROR + "; width: 60px; height: 60px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 15px;'>" +
                        "<span style='color: white; font-size: 24px;'>✕</span>" +
                        "</div>" +
                        "<h1 style='color: " + COLOR_TEXT + "; margin: 0 0 10px 0; font-size: 28px;'>Pedido Cancelado</h1>" +
                        "</div>" +
                        buildItensHtml(pedido) +
                        buildEnderecoHtml(pedido) +
                        buildSuporteFooter();

        dispararEmail(pedido.getUsuario().getEmail(), 
                     "🚫 Pedido Cancelado - Japa Universe #" + pedido.getId(), 
                     getBaseTemplate(bodyContent, "Pedido Cancelado"));
    }

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        String url = "https://japauniverse.com.br/login/HTML/nova-senha.html?token=" + token;
        String bodyContent =
                "<div style='text-align: center; margin-bottom: 30px;'>" +
                        "<h1>Redefinir Senha</h1>" +
                        "<p>Clique no botão abaixo para criar uma nova senha:</p>" +
                        "<a href='" + url + "' style='background: " + COLOR_PRIMARY + "; color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px;'>Redefinir Minha Senha</a>" +
                        "</div>";

        dispararEmail(to, "🔐 Redefinição de Senha - Japa Universe", getBaseTemplate(bodyContent, "Redefinição de Senha"));
    }

    // --- MÉTODOS AUXILIARES ---
    private String buildItensHtml(Pedido pedido) {
        StringBuilder itensHtml = new StringBuilder();
        itensHtml.append("<table width='100%' cellpadding='12' cellspacing='0' style='border: 1px solid ").append(COLOR_BORDER).append("; border-radius: 8px;'>");
        for (ItemPedido item : pedido.getItens()) {
            itensHtml.append("<tr>")
                    .append("<td>").append(item.getProduto().getNome()).append("</td>")
                    .append("<td align='center'>").append(item.getQuantidade()).append("</td>")
                    .append("<td align='right'>R$ ").append(String.format("%.2f", item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())))).append("</td>")
                    .append("</tr>");
        }
        itensHtml.append("</table>");
        return itensHtml.toString();
    }

    private String buildEnderecoHtml(Pedido pedido) {
        return "<div style='background-color: " + COLOR_BG + "; padding: 20px; border-radius: 8px; margin-top: 20px;'>" +
                "<strong>Endereço:</strong> " + pedido.getEnderecoDeEntrega().getRua() + ", " + pedido.getEnderecoDeEntrega().getNumero() + "</div>";
    }

    private String buildSuporteFooter() {
        return "<div style='text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid " + COLOR_BORDER + ";'>" +
                "<p style='font-size: 14px; color: " + COLOR_TEXT_LIGHT + ";'>Dúvidas? <a href='mailto:japauniversestore@gmail.com' style='color: " + COLOR_PRIMARY + ";'>Fale conosco</a></p>" +
                "</div>";
    }

    private String getBaseTemplate(String content, String pageTitle) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>" + pageTitle + "</title></head>" +
                "<body style='margin: 0; padding: 20px; background-color: " + COLOR_BG + "; font-family: sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background-color: " + COLOR_CARD + "; border-radius: 12px; border: 1px solid " + COLOR_BORDER + "; overflow: hidden;'>" +
                "<div style='background: #121212; padding: 30px; text-align: center;'><h1 style='color: " + COLOR_PRIMARY + "; margin: 0;'>JAPA UNIVERSE</h1></div>" +
                "<div style='padding: 40px;'>" + content + "</div>" +
                "</div></body></html>";
    }
}