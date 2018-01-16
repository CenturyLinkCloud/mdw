import React, {Component} from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import TreeView from '../../node/node_modules/react-treeview';
import '../../node/node_modules/style-loader!../../react/react-treeview.css';

class Index extends Component {
  constructor(...args) {
    super(...args);
    this.state = { rootDirs: [] };
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
      this.setState({
        rootDirs: json.dirs
      });
    });
  }
  
  render() {
    return (
      <div>
        {
          this.state.rootDirs.map(dir => {
            return (
              <TreeView key={dir.path} nodeLabel={dir.path} defaultCollapsed={false}>
                {
                  dir.files.map(file => {
                    const fileLabel = <span className="node">{file.path}</span>;
                    return (
                      <div key={file.path} className="info">{file.path}</div>
                    );
                  })
                }
              </TreeView>
            );
          })
        }
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