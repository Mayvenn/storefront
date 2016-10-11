var Stripe = {};
Stripe.setPublishableKey = function() {};
Stripe.card = {};
Stripe.card.createToken = function() {};
Stripe.applePay = {};
Stripe.applePay.checkAvailability = function() {};
/**
 * @return {!_ApplePaySession}
 */
Stripe.applePay.buildSession = function() {};

function _ApplePaySession() {};
_ApplePaySession.prototype.begin = function() {};

var ApplePaySession = {};
ApplePaySession.STATUS_SUCCESS = null;
ApplePaySession.STATUS_FAILURE = null;
