import React from '../node/node_modules/react';

const MdwContext = React.createContext({
  hubRoot: $mdwHubRoot,
  serviceRoot: $mdwServicesRoot + '/api',
  authUser: {
    // cuid has no default
    id: '', 
    name: '', 
    roles: [], 
    workgroups: []
  },
  getAuthUser: () => {
    return new Promise(resolve => {
      if (this.authUser.cuid) {
        resolve(this.authUser);
      }
      else {
        fetch(new Request(this.serviceRoot + '/AuthenticatedUser', {
          method: 'GET',
          headers: { Accept: 'application/json'},
          credentials: 'same-origin'
        }))
        .then(response => {
          return response.json();
        })
        .then(authUser => {
          resolve(authUser);
        });
      }
    });    
  }
});

export default MdwContext;