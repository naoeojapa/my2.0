-- ==================================================================================
-- ARQUIVO: BACK/src/main/resources/data.sql
-- ==================================================================================

-- 1. LIMPEZA
DELETE FROM itens_pedido;
DELETE FROM pagamentos;
DELETE FROM pedidos;
DELETE FROM produtos;
DELETE FROM categorias;
DELETE FROM marcas;
DELETE FROM _usuario;  -- <--- CORRIGIDO AQUI (era usuarios)

-- 2. INSERÇÃO DE MARCAS
INSERT INTO marcas (id, nome) VALUES 
(1, 'Nike'), (2, 'Air Jordan'), (3, 'Adidas'), (4, 'Bape'), (5, 'Asics'),
(6, 'New Balance'), (7, 'Puma'), (8, 'Timberland'), (9, 'Crocs'),
(10, 'Louis Vuitton'), (11, 'Dior'), (12, 'Yeezy');

-- 3. INSERÇÃO DE CATEGORIAS
INSERT INTO categorias (id, nome) VALUES 
(1, 'Air Max 95'), (2, 'Air Max DN'), (3, 'Air Max TN'), (4, 'Dunk'), 
(5, 'Jordan'), (6, 'Outros'), (7, 'Acessórios'), (8, 'Casual'), 
(9, 'Corrida'), (10, 'Botas'), (11, 'Chuteiras'), (12, 'Sandálias');

-- 4. USUÁRIO ADMIN (Vinicius)
-- Tabela corrigida para _usuario
INSERT INTO _usuario (nome, email, cpf, telefone, senha, role) VALUES -- <--- CORRIGIDO AQUI
('Vinicius Admin', 
 'vinicius.biancolini.tds24@gmail.com', 
 '000.000.000-00', 
 '11999999999', 
 '$2a$10$C0iH.HkG8Nh73C57GC7oT.jxzLawZbas/miJPJVP2qhTcpbZI0soq', 
 'ROLE_ADMIN');

 -- ==================================================================================
-- ARQUIVO: BACK/src/main/resources/data.sql
-- ==================================================================================

-- ... (Mantenha os DELETEs e os INSERTS de Marcas/Categorias/Usuarios que já fizemos) ...

-- 5. INSERÇÃO DE PRODUTOS (COM IMAGENS LOCAIS)

INSERT INTO produtos (
    nome, 
    descricao, 
    preco, 
    preco_original, 
    estoque, 
    marca_id, 
    categoria_id, 
    codigo_modelo, 
    imagem_url,   
    imagem_url_2,
    imagem_url_3, 
    imagem_url_4
) VALUES 

-- PRODUTO 1: Nike Dunk Panda
(
    'Nike Dunk Low Panda',
    'O clássico preto e branco que combina com tudo.',
    899.90,
    1199.90,
    50,
    1, -- Marca Nike
    4, -- Categoria Dunk
    'NIKE_DUNK_PANDA',
    'uploads/dnBlack.webp',  
    'uploads/nike-panda-costas.jpg',
    NULL,
    NULL
);