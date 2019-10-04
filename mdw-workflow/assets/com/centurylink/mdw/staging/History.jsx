import React, {Component} from '../node/node_modules/react';
import MdwContext from '../react/MdwContext';

class History extends Component {
    
  constructor(...args) {
    super(...args);

    if (this.props.match && this.props.match.params) {
      this.stagingCuid = this.props.match.params.cuid;
      this.package = this.props.match.params.package;
      this.assetName = this.props.match.params.asset;
    }

    this.state = { versions: [] };
  }

  componentDidMount() {
    if (this.package && this.assetName) {
      let url = this.context.serviceRoot + '/Versions/' + this.package + '/' + this.assetName + '?withCommitInfo=true';
      $mdwUi.clearMessage();
      $mdwUi.hubLoading(true);
      let ok = false;
      fetch(new Request(url, {
        method: 'GET',
        headers: { Accept: 'application/json', 'mdw-app-id': 'mdw-hub' },
        credentials: 'same-origin'
      }))
      .then(response => {
        $mdwUi.hubLoading(false);
        ok = response.ok;
        return response.json();
      })
      .then(json => {
        if (ok) {
          json.versions.forEach(version => {
            if (version.commitInfo && version.commitInfo.date) {
              version.commitInfo.date = new Date(version.commitInfo.date);
            }
          });
          this.setState({versions: json.versions});
        }
        else {
          $mdwUi.showMessage(json.status.message);
        }
      });
    }
  }

  render() {
    const hubRoot = this.context.hubRoot;
    return (
      <div>
        <div className="panel-heading mdw-heading" style={{borderColor:'#ddd'}}>
          <div className="mdw-heading-label">
            <div style={{marginTop:'-5px'}}>
              {'History of '}
              <a href={hubRoot + '/packages/' + this.package} style={{marginRight:'1px'}}
                onClick={e => {e.preventDefault(); location = hubRoot + '/#/packages/' + this.props.package; }}>
                {this.package}
              </a>
              {' / ' + this.assetName}
            </div>
          </div>
        </div>
        <div>
          <ul className="mdw-list">
            {
              this.state.versions.map((version, i) => {
                return (
                  <li key={i}>
                    <div className="mdw-flex-item">
                      <div className="mdw-flex-item-left" style={{width:'200px'}}>
                        <div>
                          <a href={'#/asset' + this.package + '/' + this.assetName + '/' + version.version} 
                            style={{fontWeight:'bold',fontSize:'15px'}}>
                            v{version.version}
                          </a>
                        </div>
                        {version.id &&
                          <div style={{display:'block',fontSize:'13px'}}>
                            {version.id}
                          </div>
                        }
                      </div>
                      {version.commitInfo &&
                        <div style={{width:'100%'}}>
                          <div style={{color:'#505050'}}>
                            {version.commitInfo.message}
                          </div>
                          {version.commitInfo.url &&
                            <a href="{version.commitInfo.url}">{version.commitInfo.commit}</a>
                          }
                          {!version.commitInfo.url &&
                            <span>{version.commitInfo.commit}</span>
                          }
                        </div>
                       }
                      <div className="mdw-flex-item-right" style={{color:'#505050',marginRight:'25px'}}>
                        {version.commitInfo &&
                          <div>
                            {version.commitInfo.date &&
                              <div>
                                {version.commitInfo.date.toLocaleString()}
                              </div>
                            }
                            <div>
                              {version.commitInfo.committer}
                            </div>
                          </div>
                        }
                      </div>
                    </div>
                  </li>
                );
              })
            }
          </ul>
        </div>
      </div>
    );
  }
}

History.contextType = MdwContext;
export default History; 