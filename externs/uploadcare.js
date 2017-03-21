var UPLOADCARE_PUBLIC_KEY;
var UPLOADCARE_LIVE;
var uploadcare = {};
uploadcare.tabsCss = {};
uploadcare.tabsCss.addUrl = function (styleString) {};

uploadcare.fileFrom = function (type, filename) {};
/** @return {_UploadcareFileGroupPromise} */
uploadcare.loadFileGroup = function (uuid) {};

/** @return {_UploadcarePanelPromise} */
uploadcare.openPanel = function () {};

function _UploadcarePanelPromise() {};
/** @param {_UploadcareFileHandler} fileHandler */
/** @return {_UploadcarePanelPromise} */
_UploadcarePanelPromise.prototype.done = function(fileHandler) {};

/** @param {_UploadcareFilePromiseFactory} */
function _UploadcareFileHandler (filePromiseFactory) {};

function _UploadcareFilePromiseFactory () {};
/** @return {_UploadcareFilePromise} */
_UploadcareFilePromiseFactory.prototype.promise = function() {};

function _UploadcareFilePromise () {};
/** @return {_UploadcareFilePromise} */
_UploadcareFilePromise.prototype.done = function(fileDoneHandler) {};
/** @return {_UploadcareFilePromise} */
_UploadcareFilePromise.prototype.fail = function(fileErrorHandler) {};

function _UploadcareFileGroupPromise () {};
/** @return {Array<_UploadcareFilePromise>} */
_UploadcareFileGroupPromise.prototype.files = function() {};

uploadcare.jQuery = function () {};
/** TODO: don't know how to parameterize this, so hard coding it as:
 @return _JQueryPromise */
uploadcare.jQuery.when = function () {};

_JQueryPromise = function () {};
_JQueryPromise.prototype.done = function (deferred, callback) {};
