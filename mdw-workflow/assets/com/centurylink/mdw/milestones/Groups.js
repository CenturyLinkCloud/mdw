class Groups {
    
  constructor(serviceRoot) {
    this.serviceRoot = serviceRoot;
  }
  
  /**
   * Retrieves if necessary.  Returns a promise.
   */
  getGroups() {
    return new Promise(resolve => {
      let sessionGroups = sessionStorage.getItem("mdw-milestoneGroups");
      if (sessionGroups) {
        resolve(JSON.parse(sessionGroups));
      }
      else {
        const url = this.serviceRoot + '/com/centurylink/mdw/milestones/groups';
        fetch(new Request(url, {
          method: 'GET',
          headers: { Accept: 'application/json', 'mdw-app-id': 'mdw-hub' },
          credentials: 'same-origin'
        }))
        .then(response => {
          return response.json();
        })
        .then(milestoneGroups => {
          var groups = [];
          if (milestoneGroups.groups) {
            groups = milestoneGroups.groups;
            groups.sort((g1, g2) => g1.name.localeCompare(g2.name));
          }
          sessionStorage.setItem("mdw-milestoneGroups", JSON.stringify(groups));
          resolve(groups);
        });
      }  
    });
  }
}

export default Groups; 