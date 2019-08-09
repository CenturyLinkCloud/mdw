import React, {Component} from '../node/node_modules/react';
import {Link} from '../node/node_modules/react-router-dom';
import {Glyphicon} from '../node/node_modules/react-bootstrap';
import MdwContext from '../react/MdwContext';

class UserStage extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { stagedAssets: undefined };
  }

  componentDidMount() {
    const cuid = this.props.stage.userCuid;
    const url = this.context.serviceRoot + '/com/centurylink/mdw/staging/' + cuid + '/assets';
    $mdwUi.clearMessage();
    $mdwUi.hubLoading(true);
    var ok = false;
    fetch(new Request(url, {
      method: 'GET',
      headers: { Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      $mdwUi.hubLoading(false);
      ok = response.ok;
      return response.json();
    })
    .then(json => {
      if (ok) {
        this.setState({stagedAssets: json});
      }
      else {
        $mdwUi.showMessage(json.status.message);
      }
    });
  }

  render() {
    if (this.state.stagedAssets) {
      const pkgs = Object.keys(this.state.stagedAssets);
      const cuid = this.props.stage.userCuid;
      return (
        <div>
          {pkgs.length === 0 &&
            <div>
              To add an asset to your staging area, find it 
              under <a href={this.context.hubRoot + '#/packages'}>Assets</a>, 
              and then click the Stage button.
            </div>
          }
          {pkgs.length > 0 &&
            <ul className="mdw-list" style={{marginTop:'-10px'}}>
              {
                pkgs.map((pkg, i) => {
                  return (
                    <li key={i}>
                      <a className="mdw-item-link"
                        href={this.context.hubRoot + '/#/packages/' + pkg}>
                        <Glyphicon glyph="folder-open" className="mdw-item-icon" />
                        {pkg}
                      </a>
                      <ul className="mdw-list" style={{marginLeft:'25px'}}>
                        {
                          this.state.stagedAssets[pkg].map((asset, j) => {
                            return (
                              <li key={j} style={{border:'0',padding:'5px 10px 0 10px'}}>
                                <Link className="mdw-item-link" 
                                  to={this.context.hubRoot + '/staging/' + cuid + '/assets/' + pkg + '/' + asset.name}>
                                  <Glyphicon glyph="file" className="mdw-item-icon" />
                                  {asset.name}
                                </Link>
                              </li>
                            );
                          })
                        }
                      </ul>
                    </li>
                  );
                })
              }
            </ul>
          }
        </div>
      );
    }
    else {
      return (<div></div>);
    }
  }
}

UserStage.contextType = MdwContext;
export default UserStage; 