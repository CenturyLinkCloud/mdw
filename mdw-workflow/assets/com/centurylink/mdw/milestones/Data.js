class Data {
    
  constructor(groups, milestones) {
    this.groups = groups;
    this.milestones = milestones;
    this.items = [];
    this.edges = [];
    this.idCtr = 0;
    this.depth = 0;
    this.maxDepth = 0;
    if (milestones.milestone) {
      this.isInstance = milestones.milestone.masterRequestId !== undefined;
      this.add(milestones);
    }
  }
  
  add(milestone) {
    let item = milestone.milestone;
    item.id = this.idCtr;
    item.level = this.depth;
    item.color = '#4cafea';
    if (item.group) {
      let group = item.group;
      delete item.group;
      let milestoneGroup = this.groups.find(g => g.name === group);
      if (milestoneGroup && milestoneGroup.props) {
        item.color = milestoneGroup.props.color;
      }
    }
    if (this.isInstance) {
      if (!item.activityInstance) {
        item.color = '#ffffff';
      }
      else if (!item.end) {
        if (item.activityInstance && item.activityInstance.status === 'Waiting') {
          item.end = new Date(); // otherwise timeline does not show full span
        }
        item.color = '#ffff00';
      }
      if (item.start) {
        if (item.end && (new Date(item.end).getTime() - new Date(item.start).getTime()) < 600000)  {
          item.type = 'point';
        }
      }
    }
    if (item.process) {
      item.title = item.process.name;
      if (item.activity) {
        item.title += ': ' + item.activity.id;
      }
    }
    item.content = '<div style="background-color:' + this.shade(item.color, 0.5) + ';padding:5px;">' + item.label + '</div>';
    this.depth++;
    this.items.push(item);
    if (milestone.children) {
      milestone.children.forEach(child => {
        let exist = this.items.find(it => {
          if (child.milestone.processInstance) {
            return it.processInstance && it.processInstance.id === child.milestone.processInstance.id && it.activityInstance.id == child.milestone.activityInstance.id;
          }
          else {
            return it.process.id === child.milestone.process.id && it.activity.id === child.milestone.activity.id;
          }
        });
        if (exist) {
          if (!this.edges.find(edge => edge.from === item.id && edge.to === exist.id)) {
            this.edges.push({
              from: item.id,
              to: exist.id
            });
          }
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

  shade(color, percent) {   
    var f = parseInt(color.slice(1), 16), t = percent < 0 ? 0 : 255, p = percent < 0 ? percent * -1 : percent, R = f >> 16, G = f >> 8 & 0x00FF, B = f & 0x0000FF;
    return "#" + (0x1000000 + (Math.round((t - R) * p) + R) * 0x10000 + (Math.round((t - G) * p) + G) * 0x100 + (Math.round((t - B) * p) + B)).toString(16).slice(1);
  }
}

export default Data;