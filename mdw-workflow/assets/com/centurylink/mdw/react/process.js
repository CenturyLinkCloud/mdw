// Retrieves process def and optionally instance
const process = {
  get: function(serviceBase, assetPath, instanceId, masterRequestId, callback) {
    if (assetPath) {
      this.getProcess(serviceBase, assetPath, process => {
        if (instanceId || masterRequestId) {
          this.getInstance(serviceBase, instanceId, masterRequestId, instance => {
            process.instance = instance;
            callback(process);
          });
        }
      });
    }
    else if (instanceId) {
      this.getInstance(serviceBase, instanceId, null, instance => {
        var path = instance.packageName + '/' + instance.processName;
        this.getProcess(serviceBase, path, process => {
          process.packageName = instance.packageName;
          process.instance = instance;
          callback(process);
        });
      }); 
    }
    else if (masterRequestId) {
      this.getInstance(serviceBase, null, masterRequestId, instance => {
        var path = instance.packageName + '/' + instance.processName;
        this.getProcess(serviceBase, path, process => {
          process.packageName = instance.packageName;
          process.instance = instance;
          callback(process);
        });
      }); 
    }
  },
  getProcess: function(serviceBase, assetPath, callback) {
    fetch(new Request(serviceBase + '/Workflow/' + assetPath, {
      method: 'GET',
      headers: {Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(process => {
      process.packageName = assetPath.substring(0, assetPath.indexOf('/'));
      callback(process);
    });
  },
  getInstance: function(serviceBase, instanceId, masterRequestId, callback) {
  var url = serviceBase + '/Processes';
  if (instanceId)
    url += '/' + instanceId;
  else if (masterRequestId)
    url += '?masterRequestId=' + masterRequestId + '&master=true';
    fetch(new Request(url, {
      method: 'GET',
      headers: {Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(obj => {
      if (obj.processInstances) {
        callback(obj.processInstances[0]);
      }
      else if (obj.owner == 'MAIN_PROCESS_INSTANCE') {
        this.getInstance(serviceBase, obj.ownerId, null, mainInst => {
          callback(mainInst);
        });
      }
      else {
        callback(obj);
      }
    });
  }
};

export default process; 