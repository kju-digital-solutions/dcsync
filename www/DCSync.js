
/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

var exec = require('cordova/exec');
var Q = require('at.kju.datacollector.dcsync.Q');

/*
 * Creates an instance of the plugin.
 * 
 * Important note: Creating multiple instances is expected to break the delegate
 * callback mechanism, as the native layer can only handle one  callback ID at a 
 * time.
 *
 * @constructor {DCSync}
 */

 function DCSync (){
 
	this._handlers= {
			'onSyncCompleted': [],
			'onSyncFailed': [],
			'onSyncProgress': [],
			'onLoginRequired': [],
			}
 }
 
/**
 * Listen for an event.
 *
 * The following events are supported:
 *
 *   - progress
 *   - cancel
 *   - error
 *   - completion
 *
 * @param {String} eventName to subscribe to.
 * @param {Function} callback triggered on the event.
 */ 
DCSync.prototype.on = function(eventName, callback) {
    if (this._handlers.hasOwnProperty(eventName)) {
        this._handlers[eventName].push(callback);
    }
};

/**
 * Emit an event.
 *
 * This is intended for internal use only.
 *
 * @param {String} eventName is the event to trigger.
 * @param {*} all arguments are passed to the event listeners.
 *
 * @return {Boolean} is true when the event is triggered otherwise false.
 */

DCSync.prototype.emit = function() {
    var args = Array.prototype.slice.call(arguments);
    var eventName = args.shift();

    if (!this._handlers.hasOwnProperty(eventName)) {
        return false;
    }

    for (var i = 0, length = this._handlers[eventName].length; i < length; i++) {
        this._handlers[eventName][i].apply(undefined,args);
    }

    return true;
};

 /**
 * Calls the method 'registerDelegateCallbackId' in the native layer which
 * saves the callback ID for later use. 
 * 
 * The saved callback ID will be used when the native layer wants to notify
 * the DOM asynchronously about an event of it's own, for example entering 
 * into a region.
 * 
 * @returns {Q.Promise}
 */
DCSync.prototype._registerCallback = function () {
	//this.appendToDeviceLog('registerDelegateCallbackId()');
	var d = Q.defer();
	var that = this;
	exec( 
		function(pluginResult) { that._onDelegateCallback(d, pluginResult);},
		d.reject, "DCSync", "registerCallback", []
	);

	return d.promise;
};


DCSync.prototype._onDelegateCallback = function (deferred, pluginResult) {

	//this.appendToDeviceLog('_onDelegateCallback() ' + JSON.stringify(pluginResult));

	if (pluginResult && pluginResult['eventType']) { // The native layer calling the DOM with a delegate event.
		this.emit(pluginResult['eventType'], pluginResult);
	} else if (Q.isPending(deferred.promise)) { // The callback ID registration finished, runs only once.
		deferred.resolve();
	} else { // The native layer calls back the delegate without specifying an event, coding error.
		console.error('Delegate registration promise is already been resolved, all subsequent callbacks should provide an "eventType" field.');
	}
}; 

/**
 * Wraps a Cordova exec call into a promise, allowing the client code to
 * operate with those promises instead of callbacks.
 *
 * @param {String} method The name of the method in the native layer to be
 * called by Cordova.
 *
 * @param {Array} commandArgs An array of arguments to be passed for the
 * native layer. Defaults to an empty array if omitted.
 *
* 
 * @returns {Q.Promise}
 */
DCSync.prototype._promisedExec = function (method, commandArgs) {
	var d = Q.defer();
	exec(d.resolve, d.reject, "DCSync", method , commandArgs);
	return d.promise;
};


/*
calls resultCallback with <date> of last sync
*/
DCSync.prototype.getLastSync = function() {
	return this._promisedExec('getLastSync', []);
}
/*
retrieves the number of documents in a given path
calls resultCallback with { count: <int>, unsynced: <int> }
*/
DCSync.prototype.getDocumentCount = function(path) {
	return this._promisedExec('getDocumentCount', [path]);
}
/*
calls resultCallback with <string> of a file url to the Synced File root folder including a trailing slash
*/
DCSync.prototype.getContentRootUri = function() {
	return this._promisedExec('getContentRootUri', []);
}
/*
returns a new unique (random) Content Id (cid) in the following format:
E621E1F8-C36C-495A-93FC-0C247A3E6E5F
*/
DCSync.prototype.newDocumentCid = function() {
	return this._promisedExec('newDocumentCid', []);
}
/*
Saves a document to local storage,
creates a new document or overwrites an existing one (depending on cid )
*/
DCSync.prototype.saveDocument = function(cid, path, document, files, upload ) {
	return this._promisedExec('saveDocument', [cid, path, document, files, upload]);
}
/*
Marks a document as deleted
*/
DCSync.prototype.deleteDocument = function(cid ) {
	return this._promisedExec('deleteDocument', [cid]);
}
/*
triggers a sync
*/
DCSync.prototype.performSync = function() {
	return this._promisedExec('performSync', []);
}
/*
*/
DCSync.prototype.setSyncOptions = function( options ) {
	return this._promisedExec('setSyncOptions', [options]);
}
/*
retrieves list of documents for a given path with filter options
*/
DCSync.prototype.searchDocuments = function(path, options) {
	return this._promisedExec('searchDocuments', [path, options]);
}
/*
*/


var DCSync = new DCSync();


module.exports.DCSync = DCSync;
module.exports.dcsync = DCSync;

