// Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
/**
 * TBD Incorporate blv into Master Requests tab ?
 */
'use strict';
/* global theUser */

angular.module(
    'blv',
    [ 'ui.bootstrap', 'ngSanitize', 'blvProcessSvc', 'blvDrawingSvc',
        'blvSharedData', 'blvtypes', 'mdwAttributesSvc', 'ngResource', 'mdw', 'assets',
        'infinite-scroll', 'ui.grid', 'ui.grid.edit', 'ui.grid.selection',
        'ui.grid.autoResize', 'ui.grid.resizeColumns',  'blvworkflow' ])

.controller(
    'BlvRequestsController',
    [
        '$scope',
        '$http',
        '$location',
        'mdw',
        'Requests',
        function($scope, $http, $location, mdw, Requests) {
          $scope.requests = [];

          $scope.busy = false;
          $scope.total = 0;
          $scope.selected = null;

          $scope.getNext = function() {
            if (!$scope.busy) {
              $scope.busy = true;

              if ($scope.selected !== null) {
                // request has been selected in typeahead
                $scope.requests = [ $scope.selected ];
                $scope.busy = false;
              } else {
                // retrieve the request list
                var type = $location.path() == '/blv' ? 'master' : 'service';
                var url = mdw.roots.services + '/Services/Requests?app=mdw-admin&type=' + type + '&start=' + $scope.requests.length;
                $http.get(url).error(function(data, status) {
                  console.log('HTTP ', status , ': ' , url);
                  this.busy = false;
                }).success(function(data, status, headers, config) {
                  $scope.total = data.total;
                  $scope.requests = $scope.requests.concat(data.requests);
                  $scope.busy = false;
                });
              }
            }
          };

          $scope.hasMore = function() {
            return $scope.requests.length === 0 || $scope.requests.length < $scope.total;
          };

          $scope.select = function() {
            $scope.requests = [ $scope.selected ];
          };

          $scope.change = function() {
            if ($scope.selected === null) {
              // repopulate list
              $scope.requests = [];
              $scope.getNext();
            }
          };

          $scope.find = function(typed) {
            return $http
                .get(
                    mdw.roots.services + '/Services/Users?app=mdw-admin&type=master&find=' + typed).then(function(response) {
                  return response.data.requests;
                });
          };
          $scope.goBusinessView = function(masterRequestId) {
            $location.path("/blv-business/business/" + masterRequestId);
          };
          $scope.goWorkflowView = function(masterRequestId) {
            $location.path("/blv-workflow/workflow/" + masterRequestId);
          };

        } ])

.controller('RequestController', [ '$scope', '$routeParams',
    'Requests', function($scope, $routeParams, Requests) {
      $scope.requestId = $routeParams.requestId;
      $scope.request = Requests.get({
        id : $routeParams.requestId
      });
    } ])

.factory(
        'Requests',
        [
            '$resource',
            'mdw',
            function($resource, mdw) {
              return $resource(mdw.roots.services + '/Services/Requests/:id',
                  mdw.serviceParams(), {
                    get : {
                      method : 'GET',
                      isArray : false
                    }
                  });
            } ])

    .controller(
        'blvTabsController',
        [ '$uibModalInstance', 'activityNode', 'blvViewType', '$scope', 'blvSharedData','Assets','Asset',
            function($uibModalInstance, aNode, blvViewType, $scope, blvSharedData, Assets, Asset) {
              this.getOverrideAttributesTabs = getOverrideAttributesTabs;
              blvSharedData.activityNode = aNode;
              $scope.activityNode = aNode;
              $scope.blvViewType = blvViewType;
              $scope.modal = $uibModalInstance;
              $scope.defaultTabs = [];
              if ($scope.blvViewType == 'workflow') {
                $scope.defaultTabs.push(
                {
                title : 'Override Attributes (View Only)',
                route : 'overrideattributestab',
                template : 'blv/mdwOverrideAttributes.html'
              });
              } else {
                // Add a BAM attributes tab
                $scope.defaultTabs.push(
                    {
                    title : 'BAM Live Values',
                    route : 'bamvaluestab',
                    template : 'blv/mdwBamValues.html'
                  });
              }
              $scope.defaultTabs.push( {
                title : 'BAM Design',
                route : 'blvtab',
                template : 'blv/mdwBamAttributes.html'
              });

              getOverrideAttributesTabs();
              $scope.tabs = $scope.defaultTabs;
              $scope.close = function() {
                $uibModalInstance.close("Someone Closed Me");
              };
              function getAssetsData(pkg) {
                return function(pkgData) {
                  for (var i = 0; i < pkgData.assets.length; i++) {
                    var asset = pkgData.assets[i];
                    if (asset.name.endsWith('.pagelet')) {
                      //Need to set the pagelet variable below
                      //otherwise it gets mixed up
                    var pagelet = {};
                    pagelet.name = asset.name;
                    pagelet.displayName = asset.name;
                    if ($scope.blvViewType == 'business') {
                      pagelet.displayName = _.replace(asset.name,'.pagelet','');
                    }
                    pagelet.pkgname = pkg.name;
                    pagelet.pkgversion = pkg.version;
                    pagelet.version = asset.version;
                    pagelet.assetid = asset.id;
                    // Note below, how to pass data to a success callback
                    // This gets around the Jshint error 'Don't make functions within a loop' 
                      Asset.get({
                        packageName: pkg.name,
                        assetName: asset.name
                      }, getAssetData(pagelet)
                      );
                  }
                }
                };
            }
              // Note below, how to pass data to a success callback
              // This gets around the Jshint error 'Don't make functions within a loop' 
              function getAssetData(pagelet) {
                return function(assetData) {
                  var x2js = new X2JS();
                  var jsonAttributesDef = x2js.xml_str2json(assetData.rawResponse);
                  //console.log(jsonAttributesDef.PAGELET);
                  $scope.pagelets.push({id : pagelet.assetid, title : pagelet.name, displayName : pagelet.displayName, version : pagelet.version , route: '/blv-pagelet/'+pagelet.assetid, template: 'blv/attributesTemplate.html', pkg : pagelet.pkgname, pkgversion : pagelet.pkgversion , content : jsonAttributesDef});
                };
              }
              function getOverrideAttributesTabs() {
                //Get all packages
                $scope.pagelets = [];
                blvSharedData.data.pagelets = [];
                var pkgList = Assets.get({},
                    function(pkgList) {
                    // look for pagelet assets
                    _.forEach(pkgList.packages, function(pkg) {            
                      Assets.get({
                        packageName: pkg.name}, getAssetsData(pkg));
                      });
                    //console.log($scope.pagelets);
                
                });
              }
            } ])

    .controller(
        'blvSaveController',
        [
            '$uibModalInstance',
            'activityNode',
            'response',
            '$scope',
            'blvtypes',
            function($uibModalInstance, aNode, response, $scope, blvtypes) {
              $scope.activityNode = aNode;
              $scope.close = function() {
                $uibModalInstance.close("");
              };
              // At the moment it returns null
              // TODO Fix this
              var x2js = new X2JS();
              var mdwStatusMessageJson = x2js.xml_str2json(response.data);
              $scope.blvupordown = 'down';
              $scope.bamSpecificMessage = "Undefined response from server";
              if (mdwStatusMessageJson) {
                $scope.bamSpecificMessage = 'Attributes update <b>FAILED</b>';
                if (mdwStatusMessageJson.MDWStatusMessage.StatusCode == '0') {
                  $scope.blvupordown = 'up';
                  $scope.bamSpecificMessage = 'Attributes updated <b>successfully</b>';
                  // updateActivityBAM(activity, trigger);
                }

              }

            } ])

    .controller(
        'artisController',
        [
            'blvutils',
            '$scope',
            '$http',
            '$q',
            '$uibModal',
            'blvSharedData',
            'blvtypes',
            'mdwAttributeService',
            'mdw',
            function(blvutils, $scope, $http, $q, $uibModal, blvSharedData,
                blvtypes, mdwAttributeService, mdw) {
              var self = this;
              self.activityNode = blvSharedData.activityNode;
              self.ownerId = self.activityNode.activity.processDefinition.owner;
              $scope.activityNode = self.activityNode;
              $scope.useArtis = self.activityNode.activity.useArtis;
              $scope.serviceName = self.activityNode.activity.artisServiceName;
              $scope.functionLabel = self.activityNode.activity.artisFunctionLabel;
              // this.saveArtis = saveArtis;
              $scope.saveArtis = function() {
                var attributes = {};
                attributes['OVERRIDE_' + $scope.activityNode.activity.strippedOffid + ':ARTIS@Use'] = $scope.useArtis ? 'true' : 'false';
                attributes['OVERRIDE_' + $scope.activityNode.activity.strippedOffid + ':ARTIS@FunctionLabel'] = $scope.functionLabel;
                attributes['OVERRIDE_' + $scope.activityNode.activity.strippedOffid + ':ARTIS@ServiceName'] = $scope.serviceName;

                mdwAttributeService.updateAttributes(self.ownerId, 'PROCESS',
                    attributes, handleSuccess, handleError);
              };
              function handleError(response) {
                //TBD deal with error
              }
              // I transform the successful response, unwrapping the application
              // data
              // from the API response payload.
              function handleSuccess(response) {
                var modalInstance = $uibModal.open({
                  animation : true,
                  backdrop : true,
                  keyboard : true,
                  backdropClick : true,
                  templateUrl : 'blv/blvsave.html',
                  controller : 'blvSaveController',
                  // size: 'sm',
                  resolve : {
                    activityNode : function() {
                      return self.activityNode;
                    },
                    response : function() {
                      return response;
                    },

                  }

                });
                // return( response.data );
              }

            } ])

    .controller(
        'blvValuesController',
        [
            'blvutils',
            '$scope',
            '$http',
            '$uibModal',
            'blvSharedData',
            'blvtypes',
            'mdwAttributeService',
            'mdw',
            '$interval',
            'blvGridService',
            function(blvutils, $scope, $http, $uibModal, blvSharedData,
                blvtypes, mdwAttributeService, mdw, $interval, blvGridService) {

              var self = this;
              self.activityNode = blvSharedData.activityNode;
              $scope.activityNode = self.activityNode;
              self.bamEvents = [ "Start", "Finish" ];

              self.bamEventStart = blvutils.getBamEvent(self.activityNode,
                  "Start");
              // Create dummy bamEvents for population
              if (!self.bamEventStart) {
                self.bamEventStart = new blvtypes.BamEvent('BAM@START_MSGDEF',
                    null, self.activityNode.activity.processDefinition,
                    self.activityNode.activity);
              }
              self.bamEventFinish = blvutils.getBamEvent(self.activityNode,
                  "Finish");
              if (!self.bamEventFinish) {
                self.bamEventFinish = new blvtypes.BamEvent(
                    'BAM@FINISH_MSGDEF', null,
                    self.activityNode.activity.processDefinition,
                    self.activityNode.activity);

              }
              $scope.bamEvent = self.bamEventStart;
              // blvSharedData.data.activityNode = aNode;
              // blvSharedData.data.bamEvent = self.bamEventStart;

              // Grid stuff
              $scope.gridOptions = {
                enableRowSelection : true,
                enableSelectAll : true,
                selectionRowHeaderWidth : 35,
                rowHeight : 35,
                showGridFooter : true
              };
              $scope.gridOptions.columnDefs = [ {
                name : 'name',
                displayName : 'Name',
                enableCellEdit : true
              }, {
                name : 'value',
                displayName : 'Value',
                enableCellEdit : true
              } ];

              $scope.gridOptions.onRegisterApi = function(gridApi) {
                $scope.gridApi = gridApi;
                gridApi.selection.on.rowSelectionChanged($scope, function(row) {
                  var msg = 'row selected ' + row.isSelected;
                  //console.log(msg);
                  $scope.selectedRows = _.filter(row.grid.rows, 'isSelected',
                      true);
                });
                gridApi.selection.on.rowSelectionChangedBatch($scope, function(
                    rows) {
                  var msg = 'rows changed ' + rows.length;
                  $scope.selectedRows = _.filter(rows, 'isSelected', true);
                 // console.log(msg);
                });

              };
              // Populate data in attributes table
              $scope.gridOptions.data = $scope.bamEvent.attributes;
              $scope.gridHeight = blvGridService
                  .getGridHeight($scope.gridOptions);
              $interval(function() {
                if ($scope.gridOptions.data[0]) {
                  $scope.gridApi.selection
                      .selectRow($scope.gridOptions.data[0]);
                }
              }, 0, 1);
              $scope.deleteSelectedRows = function() {
                if ($scope.selectedRows && $scope.selectedRows.length > 0) {
                  // Use lodash
                  _.forEach(
                          $scope.selectedRows,
                          function(row, index) {
                            _.remove(
                                    $scope.gridOptions.data,
                                    function(attribute) {
                                      return (attribute.name == row.entity.name && attribute.value == row.entity.value);
                                    });
                          });
                  $scope.gridHeight = blvGridService
                      .getGridHeight($scope.gridOptions);
                 // console.log($scope.gridOptions.data);
                }

              };
              $scope.addRow = function() {
                var n = $scope.gridOptions.data.length + 1;
                $scope.gridOptions.data.push({
                  "name" : "Attribute Name " + n,
                  "value" : "Attribute Value " + n
                });
                $scope.gridHeight = blvGridService
                    .getGridHeight($scope.gridOptions);
              };

              // End of Grid stuff

              $scope.changeBamEvent = function() {
                $scope.bamEvent = self["bamEvent" + $scope.selectedOption];
                // blvSharedData.data.bamEvent = $scope.bamEvent;
                $scope.gridOptions.data = $scope.bamEvent.attributes;
                $scope.gridHeight = blvGridService
                .getGridHeight($scope.gridOptions);

              };

              // var originalData = angular.copy(simpleList);
              $scope.availableOptions = [ 'Start', 'Finish' ];

              // Only clears from the scope, doesn't save anything
              $scope.clear = function() {
                $scope.bamEvent.name = '';
                $scope.bamEvent.realm = '';
                $scope.bamEvent.cat = '';
                $scope.bamEvent.subcat = '';
                $scope.bamEvent.component = '';
                // $scope.bamEvent = blvSharedData.data.bamEvent;
              };
              $scope.saveAll = function() {
                var bamdefs = buildBamMessageDefinition(
                    $scope.bamEvent.activity, $scope.selectedOption);
                var request = $http({
                  method : "post",
                  url : mdw.roots.services + '/Services/ActionRequest?format=xml',
                  data : bamdefs
                });
                return (request.then(handleSuccess, handleError));

                // mdwAttributeService.updateAttributes("BLV", "aa70413",
                // 'PROCESS',"123",$scope.bamEvent.attributes, handleSuccess,
                // handleError);
              };
              function buildBamMessageDefinition(activity, trigger) {
                /**
                 * <ActionRequest> <Action Name="SaveBamMonitoring"> <Parameter
                 * name="ActivityId">7</Parameter> <Parameter
                 * name="ProcessName">IainParallel</Parameter> <Parameter
                 * name="ProcessVersion">2</Parameter> <Parameter
                 * name="StartBamMessageDefinition"><![CDATA[<BamMsgDef>
                 * <trigger>START</trigger> <name>Start activity</name> <data></data>
                 * <realm>MyRealm</realm> <cat>CAT</cat> <subcat>SUBCAT</subcat>
                 * <attrs> <attr> <an>Route chosen</an> <av>No Idea at the
                 * moment</av> </attr> </attrs> </BamMsgDef>]]></Parameter>
                 * <Parameter name="FinishBamMessageDefinition"><![CDATA[<BamMsgDef>
                 * <trigger>FINISH</trigger> <name>Start activity</name>
                 * <data></data> <realm>MyRealm</realm> <cat>CAT</cat>
                 * <subcat>SUBCAT</subcat> <attrs> <attr> <an>Route chosen</an>
                 * <av>No Idea at the moment</av> </attr> </attrs>
                 * </BamMsgDef>]]></Parameter> </Action> </ActionRequest>
                 * 
                 */
                var xml = '<?xml version="1.0" encoding="UTF-8"?>';
                xml += '<mdw:ActionRequest xmlns:mdw="http://mdw.centurylink.com/services">';
                xml += '<Action Name="SaveBamMonitoring">';
                // Add activity Id
                xml += '<Parameter name="ActivityId">';
                xml += activity.strippedOffid;
                xml += '</Parameter>';
                // Add process Id
                xml += '<Parameter name="ProcessName">';
                xml += activity.processDefinition.name;
                xml += '</Parameter>';
                // Add process version
                xml += '<Parameter name="ProcessVersion">';
                // TBD support different versions
                // In the meantime update the latest
                // xml += activity.processDefinition.version;
                xml += '0';

                xml += '</Parameter>';
                if (trigger == 'Start') {
                  // Add StartBamMessageDefinition
                  xml += '<Parameter name="StartBamMessageDefinition">';
                  xml += getBamMessageDefinition(trigger);
                  xml += '</Parameter>';
                } else if (trigger == 'Finish') {
                  // Add FinishBamMessageDefinition
                  xml += '<Parameter name="FinishBamMessageDefinition">';
                  xml += getBamMessageDefinition(trigger);
                  xml += '</Parameter>';
                }

                xml += '</Action>';
                xml += '</mdw:ActionRequest>';
                return xml;
              }

              /**
               * <![CDATA[<BamMsgDef> <trigger>START</trigger> <name>Start
               * activity</name> <data></data> <realm>MyRealm</realm>
               * <cat>CAT</cat> <subcat>SUBCAT</subcat> <attrs> <attr>
               * <an>Route chosen</an> <av>No Idea at the moment</av> </attr>
               * </attrs> </BamMsgDef>]]>
               * 
               */
              function getBamMessageDefinition(trigger) {
                var eventName = $scope.bamEvent.name;
                var category = $scope.bamEvent.cat;
                var subcategory = $scope.bamEvent.subcat;
                var component = $scope.bamEvent.component;
                var bamrealm = $scope.bamEvent.realm;
                var attrs = $scope.gridOptions.data;
                var xml = '<![CDATA[<BamMsgDef>';
                // Add trigger
                xml += '<trigger>';
                xml += trigger.toUpperCase();
                xml += '</trigger>';
                // Add eventName
                xml += '<name>';
                xml += eventName;
                xml += '</name>';
                // Add data
                xml += '<data>';
                xml += '</data>';
                // Add realm
                xml += '<realm>';
                xml += bamrealm;
                xml += '</realm>';
                // Add Cat
                xml += '<cat>';
                xml += category;
                xml += '</cat>';
                // Add SubCat
                xml += '<subcat>';
                xml += subcategory;
                xml += '</subcat>';
                // Add Attrs
                xml += '<attrs>';
                for (var i = 0; i < attrs.length; i++) {
                  // var rowData = bamAttributes.fnGetData(attrs[i]);
                  var name = attrs[i].name;
                  var value = attrs[i].value;
                  // Add attr
                  xml += '<attr>';
                  xml += '<an>';
                  xml += name;
                  xml += '</an>';
                  xml += '<av>';
                  xml += value;
                  xml += '</av>';
                  xml += '</attr>';
                }
                xml += '</attrs>';
                xml += ' </BamMsgDef>]]>';
                return xml;

              }

              function handleError(response) {
                // The API response from the server should be returned in a
                // normalized format. However, if the request was not handled by
                // the
                // server (or what not handles properly - ex. server error),
                // then we
                // may have to normalize it on our end, as best we can.
                //TBD handl;e error
              }
              // I transform the successful response, unwrapping the application
              // data
              // from the API response payload.
              function handleSuccess(response) {
                var modalInstance = $uibModal.open({
                  animation : true,
                  backdrop : true,
                  keyboard : true,
                  backdropClick : true,
                  templateUrl : 'blv/blvsave.html',
                  controller : 'blvSaveController',
                  // size: 'sm',
                  resolve : {
                    activityNode : function() {
                      return self.activityNode;
                    },
                    response : function() {
                      return response;
                    },

                  }

                });
                // return( response.data );
              }

            } ])

    .controller(
        'BlvController',
        [
            '$scope',
            '$uibModal',
            'Processes',
            '$routeParams',
            'DrawingService',
            'blvSharedData',
            'mdw',
            'blvworkflow' ,
            '$window', 
            '$uibPosition',
            '$q',
            function($scope, $uibModal, processService, $routeParams,
                drawingService, blvSharedData, mdw, Messages, blvworkflow,$window, $uibPosition, $q) {
              blvSharedData.data.masterRequestId = $routeParams.masterRequestId;
              $scope.masterRequestId = blvSharedData.data.masterRequestId;
              $scope.blvViewType = $routeParams.blvViewType;
              console.log("Websocket status", Messages.status);

              $scope.opened = false;
              $scope.gotBam = true;
              $scope.fireCustomEvent = function() {
                $scope.opened = !$scope.opened;
              };
             // var deferredBLV = $q.defer();
             // var promise = deferredBLV.promise;
             // promise.then(function (result) {
              processService.getProcessDefinition().then(
                  function(pd) {
                    var processDefinition = pd;
                    $scope.progress = {status: 'Loading Process Definition ' + processDefinition.name, percent : 5, type : 'info'};
                    var mainProcess;
                    mdw.hubLoading(true);
                    if (processDefinition) {
                      $scope.processDefinition = processDefinition;
                      $scope.progress = {status: processDefinition.name + ' successfully loaded.', percent : 10, type : 'info'};
                      if ($scope.blvViewType == 'workflow') {
                        /**
                         * Main Workflow
                         */
                        mainProcess = drawingService
                            .drawWorkflow(processDefinition);
                      } else if ($scope.blvViewType == 'business') {
                        /**
                         * BAM workflow
                         */
                        $scope.progress = {status: 'Building business workflow for ' + processDefinition.name, percent : 15, type : 'info'};
                       mainProcess = drawingService
                            .drawBamWorkflow(processDefinition);
                       $scope.progress = {status: 'Updating historical messages for ' + processDefinition.name, percent : 65, type : 'info'};
                        
                        drawingService.updateHistoricalBAMMessages();
                        

                      }
                      blvSharedData.data.mainProcess = mainProcess;
                      var tooltipLayer = new Kinetic.Layer();
                      var tooltip = new Kinetic.Text({
                        text: "",
                        fontFamily: "Calibri",
                        fontSize: 12,
                        padding: 5,
                        textFill: "white",
                        fill: "black",
                        alpha: 0.75,
                        visible: false,
                        drawHitFunc: function (context) {}
                    });

                    var tipRect = new Kinetic.Rect({
                        x: 100,
                        y: 60,
                        stroke: '#555',
                        strokeWidth: 2,
                        fill: '#FFA',
                        width: 380,
                        height: 100,
                        shadowColor: 'black',
                        shadowBlur: 5,
                        shadowOffset: [5, 5],
                        shadowOpacity: 0.2,
                        cornerRadius: 10,
                        drawHitFunc: function (context) {},
                        visible: false
                    });
                    tipRect.off('mouseover mouseout mouseenter mouseleave mousemove mousedown mouseup');
                    tooltipLayer.add(tipRect);
                    tooltipLayer.add(tooltip);
                    if (mainProcess.myStage) {
                      mainProcess.myStage.add(tooltipLayer);
                    }

                      // Set up modals
                      angular.forEach(mainProcess.hashIds, function(
                          activityNode, key) {
                        activityNode.group.on('click', function(e) {
                          var x = e.evt.pageX;// - $(document).scrollLeft();
                          var y = e.evt.pageY;// - $(document).scrollTop();

                          // $scope.activityNode = activityNode;

                          var modalInstance = $uibModal.open({
                            animation : true,
                            backdrop : true,
                            keyboard : true,
                            backdropClick : true,
                            templateUrl : 'blv/mdwAttributes.html',
                            controller : 'blvTabsController',
                            size : 'lg',
                            resolve : {
                              activityNode : function() {
                                return activityNode;
                              },
                              blvViewType : function() {
                                return $scope.blvViewType;
                              }
                            }
                          });

                        });
                        /**
                        console.log(mainProcess);
                        */
                        activityNode.group.on('mouseover', function(event) {
                         // var mouseXY =  mainProcess.myStage.getPointerPosition();
                          // var canvasX = mouseXY.x;
                          // var canvasY = mouseXY.y;
                          var canvasX = activityNode.rectX+activityNode.rectWidth;
                          var canvasY = activityNode.rectY;
                          
                          //console.log("x=",canvasX,"y=",canvasY);
                          showTooltip(activityNode, canvasX, canvasY, tooltip, tipRect, tooltipLayer);
                        });

                        activityNode.group.on('mouseout', function (e) {
                          hideTooltip(tooltip, tipRect, tooltipLayer);
                        });


                      //  });
                      });
                      mdw.hubLoading(false);
                      $scope.progress = {status: 'Workflow complete ' + processDefinition.name, percent : 100, type : 'success'};
                    }
             //     });
              });
              $scope.refresh = function() {
                drawingService.updateHistoricalBAMMessages();
              };

              function getRolloverText(activityNode) {
                var attrs = '';

                _.forEach(activityNode.bamMessages, function(bamMessage) {
                  var eventName;
                  var eventTime;
                  if (bamMessage.EventName) {
                    eventName = bamMessage.EventName;
                  } else {
                    eventName = 'OSR';
                  }
                  if (bamMessage.EventTime) {
                    eventTime = bamMessage.EventTime;
                  } else {
                    eventTime = 'N/A';
                  }
                    if (bamMessage.AttributeNames)
                  {
                     
                    for (var i = 0; i < bamMessage.AttributeNames.length; i++) {
                      if (bamMessage.AttributeNames[i] != 'ProcessInstanceId' && bamMessage.AttributeNames[i] != 'ProcessName' && bamMessage.AttributeNames[i] != 'ProcessId' && bamMessage.AttributeNames[i] != 'ActivityId' && bamMessage.AttributeNames[i] != 'CallingProcessName' && bamMessage.AttributeNames[i] != 'CallingActivity') {
                        attrs+=bamMessage.AttributeNames[i];
                        attrs+='       ';
                        attrs+=bamMessage.AttributeValues[i];
                        //, bamMessage : bamMessage, eventName : eventName, eventTime : eventTime});
                        attrs+='\n';
                      }
                    }
                  }
                });
                if (attrs.length === 0) {
                  attrs='No Data';
                }
                return attrs;
            }
              function hideTooltip(tooltip, tipRect, tooltipLayer) {
                //console.log("hideTooltip:");
                tooltip.hide();
                tipRect.hide();
                tooltipLayer.draw();
            }

              function showTooltip(activityNode, newX, newY, tooltip, tipRect, tooltipLayer) {
                 tipRect.setPosition({x:newX, y:newY});
                tooltip.setPosition({x:newX, y:newY});
                  tooltip.setText(getRolloverText(activityNode));
                  tipRect.setWidth(tooltip.getWidth()+10);
                  tipRect.setHeight(tooltip.getHeight()+10);
                  tipRect.show();
                  tooltip.show();
                  tooltipLayer.draw();

              }              // Position the dialog correctly
             // var endtoendbtn = document.getElementById('#endtoendbtn');
             // var endtoendpopup = document.getElementById('#endtoendpopup');
            //  var mydiv = document.getElementById('#mydiv');
//              $scope.dialogposition = $uibPosition.position(mydiv);

              $scope.popupOptions = {
                  width : 300,
                  height : 400,
                  templateUrl : "blv/mdwBamValues.html",
                  resizable : true,
                  draggable : true,
                  position : {
                      top : 400,
                      left : 100
                  },
                  title : "Live Business Data",
                  hasTitleBar : true,  
                  pinned : false,
                  isShow : true,
                   onOpen : function(){
                    //console.log('hi');

                  },
                  onClose  : function(){},
                  onDragStart : function(){},
                  onDragEnd : function(){},
                  onResize : function(){}
              };

            } ])

    .factory('blvGridService', function($http, $rootScope) {

      var factory = {};

      factory.getGridHeight = function(gridOptions) {

        var length = gridOptions.data.length;
        var rowHeight = 35; // your row height
        var headerHeight = 40; // your header height
        var filterHeight = 40; // your filter height

        return length * rowHeight + headerHeight + filterHeight + "px";
      };
      factory.removeUnit = function(value, unit) {

        return value.replace(unit, '');
      };
      return factory;
    })

    .controller(
        'attributesController',
        [
            'blvutils',
            '$scope',
            '$http',
            '$uibModal',
            'blvSharedData',
            'blvtypes',
            'mdwAttributeService',
            'mdw',
            '$interval',
            'blvGridService',
            function(blvutils, $scope, $http, $uibModal, blvSharedData,
                blvtypes, mdwAttributeService, mdw, $interval, blvGridService) {

              var self = this;
              self.activityNode = blvSharedData.activityNode;
              $scope.activityNode = self.activityNode;
              self.ownerId = self.activityNode.activity.processDefinition.owner;

              // Grid stuff
              $scope.gridOptions = {
                enableRowSelection : false,
                enableSelectAll : false,
                //selectionRowHeaderWidth : 35,
                rowHeight : 35,
                showGridFooter : true
              };
              $scope.gridOptions.columnDefs = [ {
                name : 'name',
                displayName : 'Name',
                enableCellEdit : false
              }, {
                name : 'value',
                displayName : 'Value',
                enableCellEdit : false
              } ];

              $scope.gridOptions.onRegisterApi = function(gridApi) {
                $scope.gridApi = gridApi;
                gridApi.selection.on.rowSelectionChanged($scope, function(row) {
                  var msg = 'row selected ' + row.isSelected;
                  $scope.selectedRows = _.filter(row.grid.rows, 'isSelected',
                      true);
                });
                gridApi.selection.on.rowSelectionChangedBatch($scope, function(
                    rows) {
                  var msg = 'rows changed ' + rows.length;
                  $scope.selectedRows = _.filter(rows, 'isSelected', true);
                });

              };
              // Get the attributes for the activity
              mdwAttributeService.getAttributes(self.ownerId, 'PROCESS',
                   function(response) {
                    // Good response, so populate data in attributes table (only with Override attrs)
                    $scope.gridOptions.data = [];
                    _.forEach(response, function(value, key) {
                      if (key.startsWith("OVERRIDE_")) {
                        $scope.gridOptions.data.push({name :key, value:value});
                      }
                    });                   
                    //
                    //$scope.activityNode.activity.attributes;
                    $scope.gridHeight = blvGridService.getGridHeight($scope.gridOptions);
                    $interval(function() {
                      if ($scope.gridOptions.data[0]) {
                        $scope.gridApi.selection
                          .selectRow($scope.gridOptions.data[0]);
                      }
                    }, 0, 1);
                }, function(response) {
                  
                }
                );
              
              $scope.deleteSelectedRows = function() {
                if ($scope.selectedRows && $scope.selectedRows.length > 0) {
                  // Use lodash
                  _
                      .forEach(
                          $scope.selectedRows,
                          function(row, index) {
                            _
                                .remove(
                                    $scope.gridOptions.data,
                                    function(attribute) {
                                      return (attribute.name == row.entity.name && attribute.value == row.entity.value);
                                    });
                          });
                  $scope.gridHeight = blvGridService
                      .getGridHeight($scope.gridOptions);
                  //console.log($scope.gridOptions.data);
                }

              };
              $scope.addRow = function() {
                var n = $scope.gridOptions.data.length + 1;
                $scope.gridOptions.data.push({
                  "name" : "Attribute Name " + n,
                  "value" : "Attribute Value " + n
                });
                $scope.gridHeight = blvGridService
                    .getGridHeight($scope.gridOptions);
              };

              // End of Grid stuff

              $scope.saveAll = function() {
                mdwAttributeService.updateAttributes(self.ownerId, 'PROCESS',
                    $scope.gridOptions.data, handleSuccess, handleError);

              };

              function handleError(response) {
                // The API response from the server should be returned in a
                // normalized format. However, if the request was not handled by
                // the
                // server (or what not handles properly - ex. server error),
                // then we
                // may have to normalize it on our end, as best we can.
                //TBD handle error
              }
              // I transform the successful response, unwrapping the application
              // data
              // from the API response payload.
              function handleSuccess(response) {
                var modalInstance = $uibModal.open({
                  animation : true,
                  backdrop : true,
                  keyboard : true,
                  backdropClick : true,
                  templateUrl : 'blv/blvsave.html',
                  controller : 'blvSaveController',
                  // size: 'sm',
                  resolve : {
                    activityNode : function() {
                      return self.activityNode;
                    },
                    response : function() {
                      return response;
                    },

                  }

                });
                // return( response.data );
              }

            } ])
   .controller(
        'bamValuesController',
        [
            '$scope',
            'blvSharedData',
            'blvGridService',
            'DrawingService',
            function($scope, blvSharedData, blvGridService, drawingService) {
              //drawingService.updateHistoricalBAMMessages();
              // Support clicking on an activity or just rollover for all BAM messages
             // $scope.activityNode = blvSharedData.data.activityNode;
             
              if ($scope.activityNode) {
               // $scope.activityNode = activityNode;
                $scope.bamMessages = $scope.activityNode.bamMessages;
              } else {
                $scope.bamMessages = blvSharedData.data.mainProcess.bamMessages;
              }

              // Grid stuff
              $scope.highlightFilteredHeader = function( row, rowRenderIndex, col, colRenderIndex ) {
                if( col.filters[0].term ){
                  return 'header-filtered';
                } else {
                  return '';
                }
              };
              $scope.bamValueGridOptions = {
                enableRowSelection : false,
                enableSelectAll : false,
                //selectionRowHeaderWidth : 35,
                rowHeight : 35,
                showGridFooter : true,
                enableFiltering: true
              };
              $scope.bamValueGridOptions.columnDefs = [{
                name : 'eventName',
                displayName : 'Event Name',
                cellTemplate: 'blv/mdwAttributeEventNamePopover.html'
              } ,  {
                name : 'name',
                displayName : 'Name',
                cellTemplate: 'blv/mdwAttributeNamePopover.html',
                headerCellClass: $scope.highlightFilteredHeader 
              }, {
                name : 'value',
                cellTemplate: 'blv/mdwAttributeValuePopover.html',
                displayName : 'Value',
                enableCellEdit : false
              },  {
                name : 'eventTime',
                displayName : 'Event Time',
                cellTemplate: 'blv/mdwAttributeEventTimePopover.html'
              } ];

              $scope.bamValueGridOptions.onRegisterApi = function(gridApi) {
                $scope.gridApi = gridApi;
                // call resize every 500 ms for 5 s after modal finishes opening - usually only necessary on a bootstrap modal
               // $interval( function() {
                //  $scope.gridApi.core.handleWindowResize();
               // }, 500, 10);

              };

              $scope.bamValueGridOptions.data = [];
               _.forEach($scope.bamMessages, function(bamMessage) {
                 var eventName;
                 var eventTime;
                 if (bamMessage.EventName) {
                   eventName = bamMessage.EventName;
                 } else {
                   eventName = 'OSR';
                 }
                 if (bamMessage.EventTime) {
                   eventTime = bamMessage.EventTime;
                 } else {
                   eventTime = 'N/A';
                 }
                 
                  if (bamMessage.AttributeNames)
                 {
                   for (var i = 0; i < bamMessage.AttributeNames.length; i++) {
                     if (bamMessage.AttributeNames[i] != 'ProcessInstanceId' && bamMessage.AttributeNames[i] != 'ProcessName' && bamMessage.AttributeNames[i] != 'ProcessId' && bamMessage.AttributeNames[i] != 'ActivityId' && bamMessage.AttributeNames[i] != 'CallingProcessName' && bamMessage.AttributeNames[i] != 'CallingActivity') {
                       $scope.bamValueGridOptions.data.push({name :bamMessage.AttributeNames[i], value:bamMessage.AttributeValues[i], bamMessage : bamMessage, eventName : eventName, eventTime : eventTime});
                     }
                   }
                 }
               });               
              $scope.gridHeight = blvGridService.getGridHeight($scope.bamValueGridOptions);
              // End of Grid stuff
              $scope.popupOptions = {
                  width : 100,
                  height : 100,
                  templateUrl : "blv/mdwBamValues.html",
                  resizable : true,
                  draggable : true,
                  position : {
                      top : 100,
                      left : 100
                  },
                  title : "Live Business Data",
                  hasTitleBar : true,  
                  pinned : false,
                  isShow : true,
                   onOpen : function(){
                    //console.log('hi');

                  },
                  onClose  : function(){},
                  onDragStart : function(){},
                  onDragEnd : function(){},
                  onResize : function(){}
              };

 
            } ])
.controller(
    'PageletsController',
    [
        'blvutils',
        '$scope',
        '$http',
        '$uibModal',
        'blvSharedData',
        'blvtypes',
        'mdwAttributeService',
        'mdw',
        '$interval',
        'blvGridService',
        '$location',
        function(blvutils, $scope, $http, $uibModal, blvSharedData,
            blvtypes, mdwAttributeService, mdw, $interval, blvGridService, $location) {
          blvutils.mdwLog($scope.pagelet);
          blvutils.mdwLog($scope.ownerId);
          blvutils.mdwLog($scope.blvViewType);
          $scope.activityNode = blvSharedData.activityNode;
          
          // Get the override attributes for the activity
          mdwAttributeService.getAttributes($scope.ownerId, 'PROCESS',
               function(response) {
                // Good response, so populate data in attributes table (only with Override attrs)
                $scope.pageletOverrideAttributeValues = {};
                _.forEach(response, function(value, key) {
                  if (key.startsWith("OVERRIDE_")) {
                    $scope.pageletOverrideAttributeValues[key] = value;
                  }
                }); 
                $scope.gridOptions.data = [];
                _.forEach($scope.pagelet.content.PAGELET, function(value, key) {
                  console.log(key, '=',value);
                    if (_.isArray(value)) {
                    _.forEach(value, function(pagelet) {
                      
                      $scope.gridOptions.data.push({name :pagelet._NAME, value : $scope.pageletOverrideAttributeValues['OVERRIDE_'+$scope.activityNode.activity.strippedOffid + ':'+pagelet._NAME], label:pagelet._LABEL, widgetType: key});
                    });
                    } else {
                      $scope.gridOptions.data.push({name :value._NAME, value : $scope.pageletOverrideAttributeValues['OVERRIDE_'+$scope.activityNode.activity.strippedOffid + ':'+value._NAME], label:value._LABEL, widgetType: key});
                      
                    }
                });
                if ($scope.gridOptions) {
                  $scope.gridHeight = blvGridService.getGridHeight($scope.gridOptions);
                }


            }, function(response) {
              
            }
            );
          // Grid stuff

            $scope.gridOptions = {
                enableRowSelection : false,
                enableSelectAll : false,
                //selectionRowHeaderWidth : 35,
                rowHeight : 35,
                showGridFooter : false
              };
              $scope.gridOptions.columnDefs = [ {
                name : 'label',
                displayName : 'Label',
                enableCellEdit : false
              }, {
                name : 'name',
                displayName : 'Name',
                enableCellEdit : false
              }, {
                name : 'value',
                displayName : 'Value',
                enableCellEdit : true
              }
              ];

              $scope.gridOptions.onRegisterApi = function(gridApi) {
                $scope.gridApi = gridApi;

              };
          // populate data in attributes table (only with Override attrs)
                /**
                _.forEach(response, function(value, key) {
                  if (key.startsWith("OVERRIDE_")) {
                    $scope.gridOptions.data.push({name :key, value:value});
                  }
                });
                */                   
                //
 

          // End of Grid stuff

           $scope.viewAsset = function() {
             $scope.modal.close('Closed by view asset');
             $location.path("/asset/" + $scope.pagelet.pkg+"/"+$scope.pagelet.title);

           };
           $scope.saveAll = function() {
                             // Need to add OVERRIDE to name
            var attrsCopy = [];
            _.forEach($scope.gridOptions.data, function(data) {   
              attrsCopy.push({name: 'OVERRIDE_'+$scope.activityNode.activity.strippedOffid + ':'+data.name, value : data.value});
            });
            mdwAttributeService.updateAttributes($scope.ownerId, 'PROCESS',
                attrsCopy, handleSuccess, handleError);

          };
          $scope.save = function() {
            // Need to add OVERRIDE to name
            var attrsCopy = [];
            _.forEach($scope.gridOptions.data, function(data) {   
              if (data.name && data.value)
                attrsCopy.push({name: 'OVERRIDE_'+$scope.activityNode.activity.strippedOffid + ':'+data.name, value : data.value});
            });
            mdwAttributeService.updateAttributes($scope.ownerId, 'PROCESS',
                attrsCopy, handleSuccess, handleError);

          };

          function handleError(response) {
            // The API response from the server should be returned in a
            // normalized format. However, if the request was not handled by
            // the
            // server (or what not handles properly - ex. server error),
            // then we
            // may have to normalize it on our end, as best we can.
            //TBD handle error
          }
          // I transform the successful response, unwrapping the application
          // data
          // from the API response payload.
          function handleSuccess(response) {
            var modalInstance = $uibModal.open({
              animation : true,
              backdrop : true,
              keyboard : true,
              backdropClick : true,
              templateUrl : 'blv/blvsave.html',
              controller : 'blvSaveController',
              // size: 'sm',
              resolve : {
                activityNode : function() {
                  return $scope.activityNode;
                },
                response : function() {
                  return response;
                },

              }

            });
            // return( response.data );
          }

        } ]);
