var UPLOADCARE_PUBLIC_KEY;
var UPLOADCARE_LIVE;
var uploadcare = {};

/** @type {function(): !UploadcarePromise} */
uploadcare.openDialog = function(){};

function UploadcarePromise() {};
/** @type {function(): !UploadcarePromise} */
UploadcarePromise.prototype.promise = function() {};
