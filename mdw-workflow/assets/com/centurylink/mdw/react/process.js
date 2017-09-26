// Retrieves process def and optionally instance
const process = {
  get: function(serviceBase, assetPath, instanceId, callback) {
    if (assetPath) {
      this.getProcess(serviceBase, assetPath, process => {
        if (instanceId) {
          this.getInstance(serviceBase, instanceId, instance => {
            process.instance = instance;
            callback(process);
          });
        }
      });
    }
    else if (instanceId) {
      this.getInstance(serviceBase, instanceId, instance => {
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
      headers: {Accept: 'application/json'}
    }))
    .then(response => {
      return response.json();
    })
    .then(process => {
      process.packageName = assetPath.substring(0, assetPath.indexOf('/'));
      callback(process);
    });
  },
  getInstance: function(serviceBase, instanceId, callback) {
    fetch(new Request(serviceBase + '/Processes/' + instanceId, {
      method: 'GET',
      headers: {Accept: 'application/json'}
    }))
    .then(response => {
      return response.json();
    })
    .then(instance => {
      if (instance.owner == 'MAIN_PROCESS_INSTANCE') {
        this.getInstance(serviceBase, instance.ownerId, mainInst => {
          callback(mainInst);
        });
      }
      else {
        callback(instance);
      }
    });
  }
};

export default process; 