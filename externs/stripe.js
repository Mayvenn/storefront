var Stripe = function() {};
Stripe.setPublishableKey = function() {};
Stripe.card = {};
Stripe.card.createToken = function() {};

/* V3 */

var stripe = {};
/** @return {!_StripeElementFactory} */
stripe.elements = function() {};
/** @return {!_StripePromise} */
stripe.createToken = function(element, options) {};

/** @return {!_StripePaymentRequest} */
stripe.paymentRequest = function(options) {};

var _StripeElementFactory = {};
/** @return {!_StripeElement} */
_StripeElementFactory.create = function(type, config) {};

var _StripeElement = {};
_StripeElement.mount = function (selector) {};

var _StripePromise = {};
_StripePromise.then = function(callback) {};

var _StripePaymentRequest = {};
/** @return {!_StripePaymentRequest} */
_StripePaymentRequest.canMakePayment = function() {};
/** @return {!_StripePaymentRequest} */
_StripePaymentRequest.then = function(callback) {};
/** @return {!_StripePaymentRequest} */
_StripePaymentRequest.on = function(name, callback) {};
/** @return {!_StripePaymentRequest} */
_StripePaymentRequest.update = function(obj) {};

var _StripePaymentRequestEvent = {};
_StripePaymentRequestEvent.updateWith = function(options) {};
_StripePaymentRequestEvent.complete = function(errorStr) {};
_StripePaymentRequestEvent.shippingAddress = {};
_StripePaymentRequestEvent.shippingOption = {};
_StripePaymentRequestEvent.payerEmail = "";
_StripePaymentRequestEvent.token = {};
_StripePaymentRequestEvent.token.card = {};
