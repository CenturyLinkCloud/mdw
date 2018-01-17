import React, {Component} from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import DirTree from './DirTree.jsx';
import '../../node/node_modules/style-loader!./filepanel.css';

// adjust mdw-main layout
let main = document.getElementById('mdw-main');
main.style.display = 'flex';
main.style.padding = '0';

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
    console.log("SELECTION: " + JSON.stringify(selection));
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
          <div className="fp-info">
            INFO<br/>
            selected:<br/>{this.state.selected.path} 
          </div>
        </div>
        <div className="fp-right">
          <div>toolbar</div>
          <div className="fp-file">
            file contents
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