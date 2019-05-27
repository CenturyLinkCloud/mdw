class DataE2e {
    
  constructor(groups, activities) {
    this.groups = groups;
    this.activities = activities;
    this.items = [];
    this.edges = [];
    this.idCtr = 0;
    this.depth = 0;
    this.maxDepth = 0;
    this.add(activities);
  }
  
  add(activities) {
    let isInstance = activities.activityInstance;
    let item = isInstance ? activities.activityInstance : activities.activity;
    item.activityId = isInstance ? 'A' + item.activityId : item.id;
    if (isInstance) {
      item.activityInstanceId = item.id;
    }
    item.id = this.idCtr;
    item.label = isInstance ? item.activityName : item.name;
    item.level = this.depth;
    if (item.milestoneName || "" === item.milestoneName) {
      item.color = '#4cafea';
    }
    if (item.milestoneGroup) {
      let group = item.milestoneGroup;
      let foundGroup = this.groups.find(g => g.name === group);
      if (foundGroup && foundGroup.props) {
        item.color = foundGroup.props.color;
      }
    }
    if (item.processName) {
      item.title = item.processName;
      if (item.activityId) {
        item.title += ': ' + item.activityId;
      }
    }
    this.depth++;
    this.items.push(item);
    if (activities.children) {
      activities.children.forEach(child => {
        let exist = this.items.find(it => {
          if (isInstance) {
            return it.activityInstanceId === child.activityInstance.id;
          }
          else {
            return it.processId === child.activity.processId && it.activityId === child.activity.id;
          }
        });
        if (exist) {
          this.edges.push({
            from: item.id,
            to: exist.id
          });
        }
        else {
          this.edges.push({
            from: item.id,
            to: ++this.idCtr
          });
          this.add(child);
        }
      }, this);
    }
    if (this.depth > this.maxDepth) {
      this.maxDepth = this.depth;
    }
    this.depth--;
  }
}

export default DataE2e; 
