import React, {Component} from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import DirTree from './DirTree.jsx';
import FileView from './FileView.jsx';
import '../../node/node_modules/style-loader!./filepanel.css';

// adjust mdw-main layout
let main = document.getElementById('mdw-main');
main.style.padding = '0';
main.style.height = 'calc(100% - 135px)';
main.style.minHeight = null;
document.body.style.height = '100%';
document.body.style.overflowX = 'hidden';
document.body.style.overflowY = 'hidden';
document.getElementsByTagName("html")[0].style.height = '100%';

class Index extends Component {
  constructor(...args) {
    super(...args);
    this.state = { rootDirs: [], selected: {}};
    this.handleSelect = this.handleSelect.bind(this);
    this.handleInfo = this.handleInfo.bind(this);
  }
  
  componentDidMount() {
    fetch(new Request(this.getChildContext().serviceRoot + '/com/centurylink/mdw/system/filepanel', {
      method: 'GET',
      headers: { Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(json => {
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
      if (json.dirs) {
        assignTabIndexes(json.dirs);
      }
      this.setState({
        rootDirs: json.dirs
      });
    });
  }
  
  handleSelect(selection) {
    this.handleInfo(selection);
    this.setState({
      rootDirs: this.state.rootDirs,
      selected: selection
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
  
  render() {
    return (
      <div className="fp-container">
        <div className="fp-left">
          <div className="fp-dirs">
            {
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
          </div>
          <div className="fp-footer">
            <div className="fp-info">
              {this.state.selected &&
                <div id="fp-info"></div>
              }
            </div>
            <div className="fp-grep">
              <div>
                <input type="text" placeholder="Pattern" />
              </div>
              <div>
                <input type="text" placeholder="Files" />
                <button value="grep">Grep</button>
              </div>
            </div>
          </div>
        </div>
        <div className="fp-right">
          <FileView item={this.state.selected} onInfo={this.handleInfo} />
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