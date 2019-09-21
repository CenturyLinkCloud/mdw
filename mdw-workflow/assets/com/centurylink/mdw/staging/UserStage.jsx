import React, {Component} from '../node/node_modules/react';
import {Link} from '../node/node_modules/react-router-dom';
import {Glyphicon} from '../node/node_modules/react-bootstrap';
import MdwContext from '../react/MdwContext';

class UserStage extends Component {
    
  constructor(...args) {
    super(...args);
    this.collapse = this.collapse.bind(this);
    this.expand = this.expand.bind(this);
    this.isAssetSelected = this.isAssetSelected.bind(this);
    this.toggleAsset = this.toggleAsset.bind(this);
    this.isPackageSelected = this.isPackageSelected.bind(this);
    this.togglePackage = this.togglePackage.bind(this);

    this.state = { 
      stagedAssets: undefined,
      selectedPackages: [],
      selectedAssets: []
     };
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
  
  isAssetSelected(asset) {
    return this.state.selectedAssets.indexOf(asset) >= 0;
  }
  toggleAsset(asset) {
    const selectedAssets = [...this.state.selectedAssets];
    const idx = selectedAssets.indexOf(asset);
    const selectedPackages = [...this.state.selectedPackages];
    if (idx >= 0) {
      selectedAssets.splice(idx, 1);
      const pkg = asset.substring(0, asset.indexOf('/'));
      const pkgIdx = selectedPackages.indexOf(pkg);
      if (pkgIdx >= 0) {
        selectedPackages.splice(pkgIdx, 1);
      }
    }
    else {
      selectedAssets.push(asset);
    }
    this.setState({
      stagedAssets: this.state.stagedAssets,
      selectedPackages: selectedPackages,
      selectedAssets: selectedAssets
    }, () => {
      if (idx >= 0 && this.props.onDeselect()) {
        this.props.onDeselect();
      }
    });
  }

  isPackageSelected(pkg) {
    return this.state.selectedPackages.indexOf(pkg) >= 0;
  }
  togglePackage(pkg) {
    const selectedPackages = [...this.state.selectedPackages];
    const idx = selectedPackages.indexOf(pkg);
    let selectedAssets = this.state.selectedAssets.filter(asset => !asset.startsWith(pkg + '/'));
    if (idx >= 0) {
      selectedPackages.splice(idx, 1);
    }
    else {
      selectedPackages.push(pkg);
      this.state.stagedAssets[pkg].forEach(stagedAsset => {
        selectedAssets.push(pkg + '/' + stagedAsset.name);
      });
    }
    this.setState({
      stagedAssets: this.state.stagedAssets,
      selectedPackages: selectedPackages,
      selectedAssets: selectedAssets
    }, () => {
      if (idx >= 0 && this.props.onDeselect()) {
        this.props.onDeselect();
      }
    });
  }
  
  componentDidMount() {
    const cuid = this.props.stage.userCuid;
    const url = this.context.serviceRoot + '/com/centurylink/mdw/staging/' + cuid + '/assets';
    $mdwUi.clearMessage();
    $mdwUi.hubLoading(true);
    var ok = false;
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
        this.setState({
          stagedAssets: json,
          selectedPackages: [],
          selectedAssets: []
        });
      }
      else {
        $mdwUi.showMessage(json.status.message);
      }
    });
  }

  componentDidUpdate(prevProps) {
    if (this.props.allSelect !== prevProps.allSelect) {
      const selectedPackages = [];
      const selectedAssets = [];
      if (this.props.allSelect) {
        Object.keys(this.state.stagedAssets).forEach(pkg => {
          selectedPackages.push(pkg);
          this.state.stagedAssets[pkg].forEach(stagedAsset => {
            selectedAssets.push(pkg + '/' + stagedAsset.name);
          });
        });
      }
      this.setState({
        stagedAssets: this.state.stagedAssets,
        selectedPackages: selectedPackages,
        selectedAssets: selectedAssets
      });
    }
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
                        <div className="mdw-heading-checklist">
                          <input type="checkbox" style={{top:'0'}} 
                            checked={this.isPackageSelected(pkg)}
                            onChange={e => this.togglePackage(pkg)} />
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
                      <ul className={'mdw-checklist' + (collapsed ? ' collapse': '')}>
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
                                <input type="checkbox" style={{top:'-1px',left:"-1px",marginRight:'6px'}} 
                                  checked={this.isAssetSelected(pkg + '/' + asset.name)} 
                                  onChange={e => this.toggleAsset(pkg + '/' + asset.name)} />
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