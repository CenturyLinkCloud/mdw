const implementors = {
  get: function(serviceBase, callback) {
    if (this.implementors) {
      callback(this.implementors);
    }
    else {
      fetch(new Request(serviceBase + '/Implementors', {
        method: 'GET',
        headers: {Accept: 'application/json'}
      }))
      .then(response => {
        return response.json();
      })
      .then(implementors => {
        this.implementors = implementors.concat($mdwUi.pseudoImplementors);
        this.implementors.sort(function(impl1, impl2) {
          return impl1.label.localeCompare(impl2.label);
        });
        callback(this.implementors);
      });        
    }
  },
};

export default implementors;