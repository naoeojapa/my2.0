package com.store.BACK.service;

import com.resend.Resend;
import com.resend.services.emails.model.SendEmailRequest;
import com.store.BACK.model.ItemPedido;
import com.store.BACK.model.Pedido;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

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

    // ✅ AQUI ESTÁ A MUDANÇA IMPORTANTE: Seu domínio oficial
    private static final String REMETENTE_PADRAO = "Japa Universe <nao-responda@japauniverse.com.br>";

    public void enviarConfirmacaoPagamento(Pedido pedido) {
        enviarPedidoRecebido(pedido);
    }

    // 1. PEDIDO RECEBIDO
    @Async
    @Transactional
    public void enviarPedidoRecebido(Pedido pedido) {
        try {
            Resend resend = new Resend(resendApiKey);
            String dataPedido = pedido.getDataPedido().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));

            String itensHtml = buildItensHtml(pedido);
            String enderecoHtml = buildEnderecoHtml(pedido);

            String bodyContent =
                    "<div style='text-align: center; margin-bottom: 30px;'>" +
                            "<div style='background-color: " + COLOR_WARNING + "; width: 60px; height: 60px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 15px;'>" +
                            "<span style='color: white; font-size: 24px;'>🕒</span>" +
                            "</div>" +
                            "<h1 style='color: " + COLOR_TEXT + "; margin: 0 0 10px 0; font-size: 28px;'>Pedido Recebido!</h1>" +
                            "<p style='color: " + COLOR_TEXT_LIGHT + "; margin: 0; font-size: 16px;'>Obrigado pela sua compra, " + pedido.getUsuario().getNome() + "!</p>" +
                            "<p style='color: " + COLOR_TEXT_LIGHT + "; margin-top: 10px; font-size: 15px;'>Seu pedido foi registrado e estamos aguardando a confirmação do seu pagamento (PIX).</p>" +
                            "</div>" +
                            "<h3 style='color: " + COLOR_TEXT + "; margin: 30px 0 15px 0; font-size: 18px; border-bottom: 2px solid " + COLOR_BORDER + "; padding-bottom: 8px;'>Resumo do Pedido #" + pedido.getId() + "</h3>" +
                            itensHtml +
                            enderecoHtml +
                            buildSuporteFooter();

            String finalHtml = getBaseTemplate(bodyContent, "Pedido Recebido #" + pedido.getId());

            SendEmailRequest request = SendEmailRequest.builder()
                    .from(REMETENTE_PADRAO)
                    .to(pedido.getUsuario().getEmail())
                    .subject("⏳ Pedido Recebido - Aguardando Pagamento Japa Universe #" + pedido.getId())
                    .html(finalHtml)
                    .build();

            resend.emails().send(request);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erro ao enviar email (Resend): " + e.getMessage());
        }
    }

    // 2. PAGAMENTO CONFIRMADO
    @Async
    @Transactional
    public void enviarPagamentoConfirmado(Pedido pedido) {
        try {
            Resend resend = new Resend(resendApiKey);
            String dataPedido = pedido.getDataPedido().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));

            String itensHtml = buildItensHtml(pedido);
            String enderecoHtml = buildEnderecoHtml(pedido);

            String bodyContent =
                    "<div style='text-align: center; margin-bottom: 30px;'>" +
                            "<div style='background-color: " + COLOR_SUCCESS + "; width: 60px; height: 60px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 15px;'>" +
                            "<span style='color: white; font-size: 24px;'>✓</span>" +
                            "</div>" +
                            "<h1 style='color: " + COLOR_TEXT + "; margin: 0 0 10px 0; font-size: 28px;'>Pagamento Confirmado!</h1>" +
                            "<p style='color: " + COLOR_TEXT_LIGHT + "; margin: 0; font-size: 16px;'>Pagamento confirmado, " + pedido.getUsuario().getNome() + "!</p>" +
                            "</div>" +
                            "<h3 style='color: " + COLOR_TEXT + "; margin: 30px 0 15px 0; font-size: 18px; border-bottom: 2px solid " + COLOR_BORDER + "; padding-bottom: 8px;'>Itens Pagos #" + pedido.getId() + "</h3>" +
                            itensHtml +
                            enderecoHtml +
                            buildSuporteFooter();

            String finalHtml = getBaseTemplate(bodyContent, "Pagamento Confirmado #" + pedido.getId());

            SendEmailRequest request = SendEmailRequest.builder()
                    .from(REMETENTE_PADRAO)
                    .to(pedido.getUsuario().getEmail())
                    .subject("✅ Pagamento Confirmado - Japa Universe #" + pedido.getId())
                    .html(finalHtml)
                    .build();

            resend.emails().send(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 3. PEDIDO ENVIADO
    @Async
    @Transactional
    public void enviarPedidoEnviado(Pedido pedido) {
        try {
            Resend resend = new Resend(resendApiKey);
            String dataPedido = pedido.getDataPedido().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));

            String itensHtml = buildItensHtml(pedido);
            String enderecoHtml = buildEnderecoHtml(pedido);
            
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
                            "<p style='color: " + COLOR_TEXT_LIGHT + "; margin: 0; font-size: 16px;'>Seu pedido está a caminho, " + pedido.getUsuario().getNome() + "!</p>" +
                            "</div>" +
                            rastreioHtml +
                            "<h3 style='color: " + COLOR_TEXT + "; margin: 30px 0 15px 0; font-size: 18px; border-bottom: 2px solid " + COLOR_BORDER + "; padding-bottom: 8px;'>Itens Enviados #" + pedido.getId() + "</h3>" +
                            itensHtml +
                            enderecoHtml +
                            buildSuporteFooter();

            String finalHtml = getBaseTemplate(bodyContent, "Pedido Enviado #" + pedido.getId());

            SendEmailRequest request = SendEmailRequest.builder()
                    .from(REMETENTE_PADRAO)
                    .to(pedido.getUsuario().getEmail())
                    .subject("🚚 Seu Pedido Foi Enviado - Japa Universe #" + pedido.getId())
                    .html(finalHtml)
                    .build();

            resend.emails().send(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 4. PEDIDO ENTREGUE
    @Async
    @Transactional
    public void enviarPedidoEntregue(Pedido pedido) {
         try {
            Resend resend = new Resend(resendApiKey);
            String itensHtml = buildItensHtml(pedido);
            String enderecoHtml = buildEnderecoHtml(pedido);

            String bodyContent =
                    "<div style='text-align: center; margin-bottom: 30px;'>" +
                            "<div style='background-color: " + COLOR_SUCCESS + "; width: 60px; height: 60px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 15px;'>" +
                            "<span style='color: white; font-size: 24px;'>🎁</span>" +
                            "</div>" +
                            "<h1 style='color: " + COLOR_TEXT + "; margin: 0 0 10px 0; font-size: 28px;'>Pedido Entregue!</h1>" +
                            "<p style='color: " + COLOR_TEXT_LIGHT + "; margin: 0; font-size: 16px;'>Esperamos que goste, " + pedido.getUsuario().getNome() + "!</p>" +
                            "</div>" +
                            "<h3 style='color: " + COLOR_TEXT + "; margin: 30px 0 15px 0; font-size: 18px; border-bottom: 2px solid " + COLOR_BORDER + "; padding-bottom: 8px;'>Itens Entregues #" + pedido.getId() + "</h3>" +
                            itensHtml +
                            enderecoHtml +
                            buildSuporteFooter();

            String finalHtml = getBaseTemplate(bodyContent, "Pedido Entregue #" + pedido.getId());

            SendEmailRequest request = SendEmailRequest.builder()
                    .from(REMETENTE_PADRAO)
                    .to(pedido.getUsuario().getEmail())
                    .subject("🎁 Seu Pedido Foi Entregue - Japa Universe #" + pedido.getId())
                    .html(finalHtml)
                    .build();

            resend.emails().send(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 5. PEDIDO CANCELADO
    @Async
    @Transactional
    public void enviarPedidoCancelado(Pedido pedido) {
        try {
            Resend resend = new Resend(resendApiKey);
            String itensHtml = buildItensHtml(pedido);
            String enderecoHtml = buildEnderecoHtml(pedido);

            String bodyContent =
                    "<div style='text-align: center; margin-bottom: 30px;'>" +
                            "<div style='background-color: " + COLOR_ERROR + "; width: 60px; height: 60px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 15px;'>" +
                            "<span style='color: white; font-size: 24px;'>✕</span>" +
                            "</div>" +
                            "<h1 style='color: " + COLOR_TEXT + "; margin: 0 0 10px 0; font-size: 28px;'>Pedido Cancelado</h1>" +
                            "<p style='color: " + COLOR_TEXT_LIGHT + "; margin: 0; font-size: 16px;'>Seu pedido foi cancelado.</p>" +
                            "</div>" +
                            "<h3 style='color: " + COLOR_TEXT + "; margin: 30px 0 15px 0; font-size: 18px; border-bottom: 2px solid " + COLOR_BORDER + "; padding-bottom: 8px;'>Resumo #" + pedido.getId() + "</h3>" +
                            itensHtml +
                            enderecoHtml +
                            buildSuporteFooter();

            String finalHtml = getBaseTemplate(bodyContent, "Pedido Cancelado #" + pedido.getId());

            SendEmailRequest request = SendEmailRequest.builder()
                    .from(REMETENTE_PADRAO)
                    .to(pedido.getUsuario().getEmail())
                    .subject("🚫 Pedido Cancelado - Japa Universe #" + pedido.getId())
                    .html(finalHtml)
                    .build();

            resend.emails().send(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- REDEFINIÇÃO DE SENHA ---
    @Async
    public void sendPasswordResetEmail(String to, String token) {
        try {
            Resend resend = new Resend(resendApiKey);
            
            // ATENÇÃO: Link apontando para o seu FRONTEND real (ajuste se não for localhost)
            String url = "https://japauniverse.com.br/login/HTML/nova-senha.html?token=" + token;
            // Se estiver testando localmente, pode manter: 
            // String url = "http://127.0.0.1:5500/FRONT/login/HTML/nova-senha.html?token=" + token;

            String bodyContent =
                    "<div style='text-align: center; margin-bottom: 30px;'>" +
                            "<div style='background: linear-gradient(135deg, " + COLOR_PRIMARY + ", " + COLOR_PRIMARY_LIGHT + "); width: 60px; height: 60px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 15px;'>" +
                            "<span style='color: white; font-size: 24px;'>🔒</span>" +
                            "</div>" +
                            "<h1 style='color: " + COLOR_TEXT + "; margin: 0 0 10px 0; font-size: 28px;'>Redefinir Senha</h1>" +
                            "<p style='color: " + COLOR_TEXT_LIGHT + "; margin: 0; font-size: 16px; line-height: 1.5;'>Recebemos uma solicitação para redefinir a senha da sua conta.</p>" +
                            "</div>" +

                            "<div style='background-color: " + COLOR_BG + "; padding: 20px; border-radius: 12px; margin: 25px 0; text-align: center;'>" +
                            "<p style='color: " + COLOR_TEXT + "; margin: 0 0 15px 0;'>Clique no botão abaixo para criar uma nova senha:</p>" +
                            "<a href='" + url + "' style='display: inline-block; background: linear-gradient(135deg, " + COLOR_PRIMARY + ", " + COLOR_PRIMARY_LIGHT + "); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 16px; box-shadow: 0 4px 15px rgba(255, 122, 0, 0.3); transition: all 0.3s ease;'>Redefinir Minha Senha</a>" +
                            "</div>" +

                            "<div style='background-color: #fff5f5; border: 1px solid #fed7d7; border-radius: 8px; padding: 15px; margin: 20px 0;'>" +
                            "<p style='color: #c53030; margin: 0; font-size: 14px; display: flex; align-items: flex-start; gap: 8px;'>" +
                            "<span style='font-size: 16px;'>⚠️</span>" +
                            "<span><strong>Importante:</strong> Se não foi você que solicitou esta redefinição, ignore este e-mail. O link expira em 1 hora.</span>" +
                            "</p>" +
                            "</div>";

            String finalHtml = getBaseTemplate(bodyContent, "Redefinição de Senha");

            SendEmailRequest request = SendEmailRequest.builder()
                    .from(REMETENTE_PADRAO)
                    .to(to)
                    .subject("🔐 Redefinição de Senha - Japa Universe")
                    .html(finalHtml)
                    .build();

            resend.emails().send(request);
            System.out.println(">>> E-mail de Redefinição enviado para: " + to);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erro ao enviar e-mail via Resend: " + e.getMessage());
        }
    }

    // --- MÉTODOS AUXILIARES: HTML (MANTIDOS) ---
    private String buildItensHtml(Pedido pedido) {
        StringBuilder itensHtml = new StringBuilder();
        itensHtml.append("<table width='100%' cellpadding='12' cellspacing='0' style='border-collapse: separate; border-spacing: 0; margin: 20px 0; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1);'>");
        itensHtml.append("<thead>")
                .append("<tr style='background: linear-gradient(135deg, ").append(COLOR_PRIMARY).append(", ").append(COLOR_PRIMARY_LIGHT).append("); color: white;'>")
                .append("<th align='left' style='padding: 12px; font-weight: 600;'>Produto</th>")
                .append("<th align='center' style='padding: 12px; font-weight: 600;'>Qtd</th>")
                .append("<th align='right' style='padding: 12px; font-weight: 600;'>Valor</th>")
                .append("</tr>")
                .append("</thead>")
                .append("<tbody>");

        for (ItemPedido item : pedido.getItens()) {
            String precoItem = String.format("%.2f", item.getPrecoUnitario());
            BigDecimal totalItemValue = item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()));
            String totalItem = String.format("%.2f", totalItemValue);

            itensHtml.append("<tr style='background-color: white; border-bottom: 1px solid ").append(COLOR_BORDER).append(";'>")
                    .append("<td style='padding: 12px; color: ").append(COLOR_TEXT).append(";'>")
                    .append("<div style='font-weight: 600; margin-bottom: 4px;'>").append(item.getProduto().getNome()).append("</div>")
                    .append("<div style='font-size: 13px; color: ").append(COLOR_TEXT_LIGHT).append(";'>Tamanho: ").append(item.getTamanho()).append("</div>")
                    .append("</td>")
                    .append("<td align='center' style='padding: 12px; color: ").append(COLOR_TEXT).append("; font-weight: 500;'>").append(item.getQuantidade()).append("</td>")
                    .append("<td align='right' style='padding: 12px; color: ").append(COLOR_TEXT).append(";'>")
                    .append("<div style='font-weight: 600;'>R$ ").append(totalItem).append("</div>")
                    .append("<div style='font-size: 12px; color: ").append(COLOR_TEXT_LIGHT).append(";'>R$ ").append(precoItem).append(" un</div>")
                    .append("</td>")
                    .append("</tr>");
        }
        itensHtml.append("<tr style='background-color: ").append(COLOR_BG).append(";'>")
                .append("<td colspan='2' align='right' style='padding: 12px; font-weight: 600; color: ").append(COLOR_TEXT).append(";'>Total:</td>")
                .append("<td align='right' style='padding: 12px; font-weight: 700; font-size: 16px; color: ").append(COLOR_PRIMARY).append(";'>R$ ").append(String.format("%.2f", pedido.getValorTotal())).append("</td>")
                .append("</tr>");
        itensHtml.append("</tbody></table>");
        return itensHtml.toString();
    }

    private String buildEnderecoHtml(Pedido pedido) {
        return "<h3 style='color: " + COLOR_TEXT + "; margin: 30px 0 15px 0; font-size: 18px; border-bottom: 2px solid " + COLOR_BORDER + "; padding-bottom: 8px;'>Endereço de Entrega</h3>" +
                "<div style='background-color: " + COLOR_BG + "; padding: 20px; border-radius: 8px; line-height: 1.6;'>" +
                "<div style='font-weight: 600; color: " + COLOR_TEXT + "; margin-bottom: 5px;'>" + pedido.getUsuario().getNome() + "</div>" +
                "<div style='color: " + COLOR_TEXT + ";'>" + pedido.getEnderecoDeEntrega().getRua() + ", " + pedido.getEnderecoDeEntrega().getNumero() + "</div>" +
                "<div style='color: " + COLOR_TEXT + ";'>" + pedido.getEnderecoDeEntrega().getCidade() + " - " + pedido.getEnderecoDeEntrega().getEstado() + "</div>" +
                "<div style='color: " + COLOR_TEXT_LIGHT + ";'>CEP: " + pedido.getEnderecoDeEntrega().getCep() + "</div>" +
                "</div>";
    }

    private String buildSuporteFooter() {
        return "<div style='text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid " + COLOR_BORDER + ";'>" +
                "<p style='color: " + COLOR_TEXT_LIGHT + "; font-size: 14px; margin: 5px 0;'>Precisa de ajuda? <a href='mailto:suporte@japauniverse.com.br' style='color: " + COLOR_PRIMARY + "; text-decoration: none;'>Entre em contato conosco</a></p>" +
                "</div>";
    }

    private String getBaseTemplate(String content, String pageTitle) {
        return "<!DOCTYPE html>" +
                "<html lang='pt-BR'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>" + pageTitle + "</title>" +
                "<style>" +
                "@import url('https://fonts.googleapis.com/css2?family=Bebas+Neue&family=Inter:wght@400;500;600;700&display=swap');" +
                "</style>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; background-color: " + COLOR_BG + "; font-family: 'Inter', 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; line-height: 1.6; color: " + COLOR_TEXT + ";'>" +
                "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' border='0' style='background-color: " + COLOR_BG + ";'>" +
                "<tr>" +
                "<td align='center' style='padding: 40px 20px;'>" +
                // Container Principal
                "<table role='presentation' width='100%' max-width='600' cellspacing='0' cellpadding='0' border='0' style='background-color: " + COLOR_CARD + "; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08); border: 1px solid " + COLOR_BORDER + ";'>" +
                // Header
                "<tr>" +
                "<td style='background: linear-gradient(135deg, #121212, #1a1a1a); padding: 30px 20px; text-align: center;'>" +
                "<h1 style='color: " + COLOR_PRIMARY + "; margin: 0; font-family: 'Bebas Neue', sans-serif; letter-spacing: 3px; font-size: 32px; font-weight: 400;'>JAPA UNIVERSE</h1>" +
                "<p style='color: rgba(255,255,255,0.7); margin: 8px 0 0 0; font-size: 14px; letter-spacing: 1px;'>STYLE • CULTURE • IDENTITY</p>" +
                "</td>" +
                "</tr>" +
                // Conteúdo
                "<tr>" +
                "<td style='padding: 40px 30px;'>" +
                content +
                "</td>" +
                "</tr>" +
                // Footer
                "<tr>" +
                "<td style='background: linear-gradient(135deg, #f8f9fa, #e9ecef); padding: 25px 20px; text-align: center; border-top: 1px solid " + COLOR_BORDER + ";'>" +
                "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' border='0'>" +
                "<tr>" +
                "<td align='center'>" +
                "<div style='margin-bottom: 15px;'>" +
                "<a href='https://www.instagram.com/japauniverse' style='display: inline-block; margin: 0 8px; color: " + COLOR_TEXT_LIGHT + "; text-decoration: none; font-size: 13px;'>Instagram</a>" +
                "<span style='color: " + COLOR_BORDER + ";'>•</span>" +
                "<a href='https://www.facebook.com/japauniverse' style='display: inline-block; margin: 0 8px; color: " + COLOR_TEXT_LIGHT + "; text-decoration: none; font-size: 13px;'>Facebook</a>" +
                "<span style='color: " + COLOR_BORDER + ";'>•</span>" +
                "<a href='mailto:contato@japauniverse.com.br' style='display: inline-block; margin: 0 8px; color: " + COLOR_TEXT_LIGHT + "; text-decoration: none; font-size: 13px;'>Contato</a>" +
                "</div>" +
                "<p style='margin: 0; font-size: 12px; color: " + COLOR_TEXT_LIGHT + ";'>" +
                "&copy; 2025 Japa Universe. Todos os direitos reservados.<br>" +
                "São Carlos - SP • Brasil" +
                "</p>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</body>" +
                "</html>";
    }
}