import React, {Component} from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import DirTree from './DirTree.jsx';

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
              <DirTree key={dir.path} dir={dir} />
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