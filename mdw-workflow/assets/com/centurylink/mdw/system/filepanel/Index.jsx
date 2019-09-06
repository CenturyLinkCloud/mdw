import React, {Component} from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import {Glyphicon} from '../../node/node_modules/react-bootstrap';
import TreeView from '../../node/node_modules/react-treeview';
import '../../node/node_modules/style-loader!../../react/react-treeview.css';
import DirTree from './DirTree.jsx';
import FileView from './FileView.jsx';
import Grep from './Grep.jsx';
import GrepResults from './GrepResults.jsx';
import '../../node/node_modules/style-loader!./filepanel.css';

// adjust mdw-main layout
let main = document.getElementById('mdw-main');
main.style.padding = '0';
main.style.height = 'calc(100% - 135px)';
main.style.minHeight = '400px';
document.body.style.overflowX = 'hidden';
document.body.style.overflowY = 'hidden';

class Index extends Component {
  constructor(...args) {
    super(...args);
    this.state = { hosts: [], rootDirs: [], selected: {}, grep: {} };
    this.handleSelect = this.handleSelect.bind(this);
    this.handleInfo = this.handleInfo.bind(this);
    this.handleGrep = this.handleGrep.bind(this);
    this.handleResultClick = this.handleResultClick.bind(this);
    this.setDirPane = this.setDirPane.bind(this);
    this.isSplitterHover = this.isSplitterHover.bind(this);
    this.onMouseMove = this.onMouseMove.bind(this);
    this.onMouseOut = this.onMouseOut.bind(this);
    this.onMouseDown = this.onMouseDown.bind(this);
    this.onMouseUp = this.onMouseUp.bind(this);
  }
  
  componentDidMount() {
    $mdwUi.clearMessage();
    var ok = false;
    fetch(new Request(this.getChildContext().serviceRoot + '/com/centurylink/mdw/system/filepanel', {
      method: 'GET',
      headers: { Accept: 'application/json', 'mdw-app-id': 'mdw-hub' },
      credentials: 'same-origin'
    }))
    .then(response => {
      ok = response.ok;
      return response.json();
    })
    .then(json => {
      if (ok) {
        var tabIndex = 100;
        let assignTabIndexes = dirs => {
          dirs.forEach(dir => {
            dir.tabIndex = tabIndex++;
            if (dir.dirs) {
              assignTabIndexes(dir.dirs);
            }
            if (dir.files) {
              dir.files.forEach(file => {
                file.tabIndex = tabIndex++;
                file.isFile = true;
              });
            }
          });
        };
        if (json.hosts) {
          json.hosts.forEach(host => {
            if (host.dirs) {
              assignTabIndexes(host.dirs);
            }
          });
        }
        if (json.dirs) {
          assignTabIndexes(json.dirs);
        }
        this.setState({
          hosts: json.hosts,
          rootDirs: json.dirs
        });
      }
      else {
        $mdwUi.showMessage(json.status.message);
      }
    });
  }
  
  handleSelect(selection) {
    $mdwUi.clearMessage();
    this.handleInfo(selection);
    this.setState({
      rootDirs: this.state.rootDirs,
      selected: selection,
      grep: {},
      lineMatch: {}
    });
  }
  
  handleInfo(item) {
      let info = '';
      if (item.isFile) {
        info += item.path.substring(0, item.path.length - item.name.length);
        info += '\n' + item.name;
      }
      else {
        info += item.path;
      }
      
      info += '\n';
      
      if (item.size) {
        var kb = (item.size / 1024);
        if (kb > 100) {
          kb = Math.round(kb);
        }
        else if (kb > 10) {
          kb = Math.round(kb * 10)/10;
        }
        else {
          kb = Math.round(kb * 100)/100;
        }
        info += kb + ' kb ';
      }
      if (item.modified) {
        info += new Date(item.modified).toLocaleString();
      }
      document.getElementById('fp-info').innerHTML = info;
  }
  
  handleGrep(find, glob) {
    $mdwUi.clearMessage();
    var ok = false;
    var path = this.state.selected.path;
    if (this.state.selected.isFile) {
      path = path.substring(0, path.length - this.state.selected.name.length - 1);
    }
    var url = this.getChildContext().serviceRoot + '/com/centurylink/mdw/system/filepanel';
    url += '?path=' + encodeURIComponent(path) + '&grep=' + find + '&glob=' + glob;
    if (this.state.selected.host) {
      url += '&host=' + this.state.selected.host;
    }
    fetch(new Request(url, {
      method: 'GET',
      headers: { Accept: 'application/json', 'mdw-app-id': 'mdw-hub' },
      credentials: 'same-origin'
    }))
    .then(response => {
      ok = response.ok;
      return response.json();
    })
    .then(json => {
      if (ok) {
        if (json.count && json.count >= json.limit) {
          $mdwUi.showMessage('Displaying first ' + json.count + ' results');
        }
        this.setState({
          hosts: this.state.hosts,
          rootDirs: this.state.rootDirs,
          selected: this.state.selected,
          grep: { results: json.results }
        });
      }
      else {
        $mdwUi.showMessage(json.status.message);
      }
    });
  }
  
  handleResultClick(file, lineMatch) {
    $mdwUi.clearMessage();
    var name = file;
    const lastSlash = file.lastIndexOf('/');
    if (lastSlash > 0) {
      name = file.substring(lastSlash + 1);
    }
    this.setState({
      rootDirs: this.state.rootDirs,
      selected: {
        path: file, 
        isFile: true, 
        name: name, 
        host: this.state.selected.host
      },
      grep: {},
      lineMatch: lineMatch
    });
  }

  setDirPane(dirPane) {
    this.dirPane = dirPane;
  }

  isSplitterHover(e) {
    if (this.dirPane) {
      let x = e.clientX - e.currentTarget.getBoundingClientRect().left;
      let dirPaneWidth = this.dirPane.offsetWidth - 2;
      return Math.abs(x - dirPaneWidth) <= 3;
    }
  }

  onMouseMove(e) {
    if (!this.isBusy) {
      this.isBusy = true;
      if (this.isSplitterDrag) {
        e.preventDefault();
        let x = e.clientX - e.currentTarget.getBoundingClientRect().left;
        this.dirPane.style.width = x + 'px';
        this.dirPane.style.minWidth = x + 'px';
        this.dirPane.style.maxWidth = x + 'px';
      }
      else {
        if (this.isSplitterHover(e)) {
          document.body.style.cursor = 'ew-resize';
        }
        else {
          document.body.style.cursor = 'default';
        }
      }
      this.isBusy = false;
    }
  }

  onMouseDown(e) {
    this.isSplitterDrag = this.isSplitterHover(e);
  }

  onMouseUp() {
    this.isSplitterDrag = false;
    document.body.style.cursor = 'default';
  }
  
  onMouseOut() {
    if (!this.isSplitterDrag) {
      document.body.style.cursor = 'default';
    }
  }

  render() {
    return (
      <div className="fp-container"
        onMouseMove={this.onMouseMove}
        onMouseOut={this.onMouseOut}
        onMouseDown={this.onMouseDown}
        onMouseUp={this.onMouseUp}>
        <div className="fp-left" ref={this.setDirPane}>
          <div className="fp-dirs">
            {this.state.rootDirs &&
              this.state.rootDirs.map(dir => {
                return (
                  <DirTree 
                    key={dir.path} 
                    dir={dir}
                    onSelect={this.handleSelect}
                    root={true} 
                    selected={this.state.selected} />
                );
              })
            }
            {this.state.hosts &&
              this.state.hosts.map(host => {
                const hostLabel = (
                  <span className="fp-item" style={{cursor:'default'}}>
                    <Glyphicon glyph="unchecked" className="fp-item-icon" 
                      style={{paddingRight:'4px'}}/>
                    {host.name}
                  </span>
                );
                return (
                  <TreeView 
                    key={host.name} 
                    nodeLabel={hostLabel}
                    defaultCollapsed={false}>
                    {host.dirs &&
                      host.dirs.map(dir => {
                        dir.host = host.name;
                        return (
                          <DirTree 
                            key={dir.name}
                            dir={dir} 
                            onSelect={this.handleSelect}
                            root={true}
                            selected={this.state.selected}
                            host={host.name} />
                        );
                      })
                    }
                  </TreeView>
                );
              })
            }
          </div>
          <div className="fp-footer">
            <div className="fp-info">
              {this.state.selected &&
                <div id="fp-info"></div>
              }
            </div>
            <Grep onGrep={this.handleGrep} item={this.state.selected} />
          </div>
        </div>
        <div className="fp-right">
          {this.state.grep.results &&
            <GrepResults item={this.state.selected}
              results={this.state.grep.results} 
              onResultClick={this.handleResultClick} />
          }
          {!this.state.grep.results &&
            <FileView item={this.state.selected} 
              onInfo={this.handleInfo} 
              lineMatch={this.state.lineMatch} />
          }
        </div>
      </div>
    );
  }
  
  getChildContext() {
    return {
      hubRoot: $mdwHubRoot,
      serviceRoot: $mdwServicesRoot + '/services'
    };
  }
}

Index.childContextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Index; 