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
    this.state = { rootDirs: [], selected: {} };
    this.handleSelect = this.handleSelect.bind(this);
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
    selection.info = '';
    if (selection.isFile) {
      selection.info += selection.path.substring(0, selection.path.length - selection.name.length);
      selection.info += '\n' + selection.name;
    }
    else {
      selection.info += selection.path;
    }
    
    selection.info += '\n';
    
    if (selection.size) {
      var kb = (selection.size / 1024);
      if (kb > 100) {
        kb = Math.round(kb);
      }
      else if (kb > 10) {
        kb = Math.round(kb * 10)/10;
      }
      else {
        kb = Math.round(kb * 100)/100;
      }
      selection.info += kb + ' kb ';
    }
    if (selection.modified) {
      selection.info += new Date(selection.modified).toLocaleString();
    }
    
    this.setState({
      rootDirs: this.state.rootDirs,
      selected: selection
    });
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
                    root={true} />
                );
              })
            }
          </div>
          <div className="fp-footer">
            <div className="fp-info">
              {this.state.selected &&
                <div>{this.state.selected.info}</div>
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
          <div style={{height:'50px'}}>toolbar</div>
          <div className="fp-file">
            <FileView item={this.state.selected} />
          </div>
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