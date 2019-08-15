import React, {Component} from '../node/node_modules/react';
import {Link} from '../node/node_modules/react-router-dom';
import {Glyphicon} from '../node/node_modules/react-bootstrap';
import MdwContext from '../react/MdwContext';

class UserStage extends Component {
    
  constructor(...args) {
    super(...args);
    this.collapse = this.collapse.bind(this);
    this.expand = this.expand.bind(this);

    this.state = { stagedAssets: undefined };
  }

  collapse(pkg) {
    if (this.props.onCollapse) {
      this.props.onCollapse(pkg);
    }

  }
  expand(pkg) {
    if (this.props.onExpand) {
      this.props.onExpand(pkg);
    }
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
        // initialize packageCollapsedState
        let packageCollapsedState = {};
        const pkgCollapsedSessionVal = sessionStorage.getItem('stagingPkgCollapsedState');
        if (pkgCollapsedSessionVal) {
          packageCollapsedState = JSON.parse(pkgCollapsedSessionVal);
        }    
        Object.keys(json).forEach(pkg => {
          if (typeof packageCollapsedState[pkg] === 'undefined') {
            packageCollapsedState[pkg] = false;
          }
        });
        sessionStorage.setItem('stagingPkgCollapsedState', JSON.stringify(packageCollapsedState));
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
            <div style={{padding:'10px'}}>
              To add an asset to your staging area, find it 
              under <a href={this.context.hubRoot + '#/packages'}>Assets</a>, 
              and then click the Stage button.
            </div>
          }
          {pkgs.length > 0 &&
            <div>
              {
                pkgs.map((pkg, i) => {
                  let collapsed = this.props.packageCollapse[pkg];
                  return (
                    <div className="mdw-sub" key={i}>
                      <div className="panel-heading mdw-sub-heading">
                        <div className="mdw-heading">
                          {pkg}
                          {!collapsed &&
                            <span>
                              {' '}
                              <a href={this.context.hubRoot + '/staging'} 
                                onClick={e => {e.preventDefault(); this.collapse(pkg); }}>
                                <Glyphicon className="mdw-action-icon button button-primary" glyph="chevron-up" />
                              </a>
                            </span>
                          }
                          {collapsed &&
                            <span>
                              {' '}
                              <a href={this.context.hubRoot + '/staging'}
                                onClick={e => {e.preventDefault(); this.expand(pkg); }}>
                                <Glyphicon className="mdw-action-icon button button-primary" glyph="chevron-down" />
                              </a>
                            </span>
                          }                        
                        </div>
                      </div>
                      <ul className={'mdw-list' + (collapsed ? ' collapse': '')}>
                        {
                          this.state.stagedAssets[pkg].map((asset, j) => {
                            let diffSymbol = '';
                            let diffClass = '';
                            if (asset.vcsDiffType) {
                              if (asset.vcsDiffType === 'DIFFERENT') {
                                diffSymbol = ' *';
                                diffClass = ' mdw-warn';
                              }
                              else if (asset.vcsDiffType === 'MISSING') {
                                diffSymbol = ' -';
                                diffClass = ' mdw-ghost';
                              }
                              else if (asset.vcsDiffType === 'EXTRA') {
                                diffSymbol = ' +';
                                diffClass = ' mdw-okay';
                              }
                            }
                            return (
                              <li key={j} style={{border:'1px solid #E8E8E8'}}>
                                <Link className={'mdw-item-link' + diffClass}
                                  to={this.context.hubRoot + '/staging/' + cuid + '/assets/' + pkg + '/' + asset.name}>
                                  {asset.name}
                                </Link>
                                {diffSymbol &&
                                  <span className={diffClass}>{diffSymbol}</span>
                                }
                              </li>
                            );
                          })
                        }
                      </ul>
                    </div>
                  );
                })
              }
            </div>
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