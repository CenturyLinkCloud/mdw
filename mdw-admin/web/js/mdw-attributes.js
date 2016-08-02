/**
 * 
 */
'use strict';

var mdwAttributesSvc = angular.module('mdwAttributesSvc', [ 'mdw' ]);

mdwAttributesSvc.service("mdwAttributeService", [
    '$http',
    'mdw',
    'Attributes',
    function($http, mdw, Attributes) {
      // Return public Api.
      return ({
        updateAttributes : updateAttributes,
        getAttributes : getAttributes
      });

      // ---
      // Public Methods
      // ---

      // Call UpdateAttributes on the server
      // Provided callbacks for success and failure
      function updateAttributes(ownerid, ownertype, attributes, successCB,
          errorCB) {

        Attributes.update({
          ownerType : ownertype,
          ownerId : ownerid,
          updateOnly : 'updateOnly'
        }, Attributes.shallowCopy({}, attributes), successCB, errorCB);
      }
      function getAttributes(ownerid, ownertype, successCB, errorCB) {
        Attributes.get({
          ownerType : ownertype,
          ownerId : ownerid
        }, {}, successCB, errorCB);

      }
      
    } ]);

mdwAttributesSvc.factory('Attributes', [
    '$resource',
    'mdw',
    function($resource, mdw) {
      return angular.extend({}, $resource(mdw.roots.services + '/Services/Attributes/:ownerType/:ownerId/:updateOnly', mdw.serviceParams(), {
        update : {
          method : 'PUT'
        },
        get : {
          method : 'GET'
        }
      }), {
        shallowCopy : function(destAttrs, srcAttrs) {
           _.forEach(srcAttrs, function(attr) {
             destAttrs[attr.name] = attr.value;
          }); 
          return destAttrs;
          }
      });
    } ]);
