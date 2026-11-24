package com.pixstop.mobile.core.platform

/**
 * Se o app pode vender pixels dentro dele.
 *
 * Na App Store, crédito comprado dentro do app tende a ser lido como moeda
 * digital, que só pode ser vendida pela compra da própria Apple (regra 3.1.1).
 * No iPhone a carteira mostra os pixels e o extrato, e a compra fica no site;
 * no Android segue como está.
 */
expect val sellsPixelsInApp: Boolean
