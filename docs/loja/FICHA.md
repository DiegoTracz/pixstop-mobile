# Ficha das lojas — Pixelstop 1.0

O texto que vai na App Store e no Google Play, pronto para colar. As imagens
estão em `capturas/` e são refeitas por `ferramentas/molduras.py` a partir de
`capturas/originais/`.

## Identidade

| Campo | Valor |
|---|---|
| Nome | Pixelstop |
| Subtítulo (App Store, 30) | Loja da geladeira da empresa |
| Bundle / pacote | `com.pixstop.mobile` |
| Categoria principal | Compras |
| Categoria secundária | Comida e bebida |
| Preço | Grátis, sem compras dentro do app |
| Disponibilidade | Só Brasil |
| Copyright | 2026 IMMO Tecnologia e Soluções Digitais Ltda. |
| URL de suporte | https://pixelstop.com.br/suporte |
| URL de marketing | https://pixelstop.com.br |
| Política de privacidade | https://pixelstop.com.br/privacidade |
| Exclusão de conta (Play) | https://pixelstop.com.br/exclusao-de-conta |

## Texto promocional (App Store, 170)

Paga pelo celular, a porta abre e você retira na hora. Use os pixels que a sua
empresa dá de benefício e suba de nível a cada compra.

## Descrição

O Pixelstop é a loja da geladeira inteligente da sua empresa. Você escolhe pelo
celular, paga e a porta abre para você retirar — sem fila, sem caixa e sem
precisar de dinheiro trocado.

COMPRE PELO CELULAR
• Veja o que tem na geladeira agora, com preço e quantidade
• Busque e filtre por bebidas, lanches e snacks
• Compre um item em dois toques ou monte o carrinho

PAGUE DO SEU JEITO
• Pix ou cartão, com o cartão guardado para a próxima
• Pixels: o benefício que a sua empresa dá vira desconto na compra

A PORTA ABRE PARA VOCÊ
• Pagou, a geladeira libera a porta
• Acompanhe o pedido até a retirada

GANHE XP E SUBA DE NÍVEL
• Missões da semana e XP a cada dia de compra
• Cada nível paga pixels
• Ranking do seu time

VIRE UM PERSONAGEM 8-BIT
• Sua foto vira avatar em pixel art

O Pixelstop funciona nas empresas que têm uma geladeira Pixelstop. Para entrar,
use o código ou o QR Code de convite da sua empresa.

## Palavras-chave (App Store, 100)

geladeira,mercado,autoatendimento,lanche,empresa,benefício,pix,snack,bebida,minimercado

## Google Play

- **Descrição curta (80):** A loja da geladeira da sua empresa: pague pelo celular e retire na hora.
- **Descrição completa:** a mesma da App Store.
- **Gráfico de destaque (1024x500):** ainda não feito.

## Informações para a revisão (App Store e Play)

**Login:** `revisao@pixelstop.com.br` (gestora) e `revisao.cliente@pixelstop.com.br`
(cliente). A senha é a de `STORE_REVIEW_PASSWORD` no `.env` da produção —
digitada à mão no App Store Connect, nunca neste arquivo.

**Contato:** nome, telefone e e-mail de quem responde à Apple.

**Notas (em inglês, colar como está):**

> Pixelstop is the companion app for smart self-service fridges installed inside
> companies (offices, factories). An employee picks products on the phone, pays,
> and the fridge door unlocks so they can take the items.
>
> HOW TO TEST
> 1. Sign in with the account above. It belongs to "Pixelstop Demonstração", a
>    demo company with a simulated fridge: there is no hardware, so after paying
>    the order is immediately marked as opened and picked up.
> 2. Tap "Loja", add any product to the cart, open the cart and tap
>    "Finalizar compra".
> 3. The payment method is "Só pixels" (pixels only). Tap "Concluir pedido".
>    The order screen shows "Pago · Produto retirado" (paid, picked up).
> No real money is charged in this account.
>
> PAYMENTS (Guideline 3.1.3(e))
> Everything sold in the app is a physical good (drinks and snacks) taken from a
> physical fridge. Real customers pay with Pix or card through Mercado Pago.
>
> PIXELS
> Pixels are an employer-funded benefit, like a meal allowance: the company
> credits them to employees, and they only discount physical products in the
> company's fridge. Pixels cannot be bought in the iOS app and cannot be
> exchanged for money.
>
> SUBSCRIPTION
> Companies contract the fridge service as a B2B agreement on our website. The
> app shows no plan prices and no links to purchase it.
>
> XP AND LEVELS
> XP is earned on fixed, announced rules (one purchase per day, three days a
> week, Friday bonus). There is no randomness tied to purchases.
>
> ACCOUNT DELETION
> Profile › "Excluir a conta", or https://pixelstop.com.br/exclusao-de-conta
>
> A short video of the real fridge opening: [link do vídeo]

## Privacidade (rótulos da App Store / Segurança dos dados do Play)

Nada é usado para rastrear a pessoa entre apps de outras empresas. Nenhum dado
vai para publicidade. Todos abaixo são **vinculados à identidade** e usados só
para **funcionalidade do app**.

| Tipo da Apple | O que é no Pixelstop |
|---|---|
| Informações de contato › Nome | nome da conta |
| Informações de contato › E-mail | login e avisos |
| Informações financeiras › Informações de pagamento | cartão enviado ao Mercado Pago para gerar o token (o app não guarda o número) |
| Compras › Histórico de compras | pedidos na geladeira |
| Conteúdo do usuário › Fotos ou vídeos | foto do avatar; foto da nota fiscal, para quem abastece |
| Identificadores › ID do usuário | id da conta no servidor |
| Identificadores › ID do dispositivo | token de push (Firebase) |

Não coletados: localização, contatos, saúde, histórico de navegação, dados de
diagnóstico e de uso para análise.

## Classificação etária

Todas as perguntas de conteúdo: **Nenhum**. Sem acesso irrestrito à web, sem
conteúdo gerado por usuários entre desconhecidos, sem apostas nem jogos de azar
(o XP segue regras fixas). Resultado esperado: **4+** na App Store e **Livre**
no Play.

## Antes de enviar para revisão

- [ ] Deploy do servidor com o `StoreReviewSeeder`, `/suporte` e o dia de sorte na sexta
- [ ] Na produção: `SUPPORT_EMAIL`, `STORE_REVIEW_PASSWORD` e `php artisan db:seed --class=StoreReviewSeeder --force`
- [ ] A caixa `suporte@pixelstop.com.br` existindo e lida
- [ ] Vídeo da geladeira real abrindo (link não listado) nas notas
- [ ] Senha e contato no App Store Connect, à mão
