import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Link} from '../node/node_modules/react-router-dom';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import {AsyncTypeahead, Menu, Highlighter} from '../node/node_modules/react-bootstrap-typeahead';
import MdwContext from '../react/MdwContext';
import HelpButton from '../react/HelpButton.jsx';
import Confirm from '../react/Confirm.jsx';
import Enter from '../react/Enter.jsx';
import StagesPopButton from './StagesPopButton.jsx';
import NewAsset from './NewAsset.jsx';

class UserStage extends Component {
    
  constructor(...args) {
    super(...args);

    this.collapse = this.collapse.bind(this);
    this.expand = this.expand.bind(this);
    this.collapseAll = this.collapseAll.bind(this);
    this.expandAll = this.expandAll.bind(this);
    this.isAssetSelected = this.isAssetSelected.bind(this);
    this.toggleAsset = this.toggleAsset.bind(this);
    this.isPackageSelected = this.isPackageSelected.bind(this);
    this.togglePackage = this.togglePackage.bind(this);
    this.toggleAllSelect = this.toggleAllSelect.bind(this);
    this.handleStage = this.handleStage.bind(this);
    this.handleUnstage = this.handleUnstage.bind(this);
    this.handleConfirmUnstage = this.handleConfirmUnstage.bind(this);
    this.doUnstage = this.doUnstage.bind(this);
    this.handlePromote = this.handlePromote.bind(this);
    this.handleConfirmPromote = this.handleConfirmPromote.bind(this);
    this.doPromote = this.doPromote.bind(this);
    this.findAssets = this.findAssets.bind(this);
    this.renderAssetMenu = this.renderAssetMenu.bind(this);

    this.confirmUnstageDialog = React.createRef();
    this.confirmPromoteDialog = React.createRef();

    this.state = {
      stagedAssets: undefined,
      selectedPackages: [],
      selectedAssets: [],
      allSelected: false,
      foundAssets: []
    };
  }

  collapse(pkg) {
    let pkgCollapsed = this.getPackageCollapsedState();
    pkgCollapsed[pkg] = true;
    this.setPackageCollapsedState(pkgCollapsed);
  }

  isCollapsed(pkg) {
    return this.getPackageCollapsedState()[pkg];
  }

  expand(pkg) {
    let pkgCollapsed = this.getPackageCollapsedState();
    pkgCollapsed[pkg] = false;
    this.setPackageCollapsedState(pkgCollapsed);
  }

  collapseAll() {
    let pkgCollapsed = this.getPackageCollapsedState();
    Object.keys(pkgCollapsed).forEach(pkgName => {
      pkgCollapsed[pkgName] = true;
    });
    this.setPackageCollapsedState(pkgCollapsed);
  }

  expandAll() {
    let pkgCollapsed = this.getPackageCollapsedState();
    Object.keys(pkgCollapsed).forEach(pkgName => {
      pkgCollapsed[pkgName] = false;
    });
    this.setPackageCollapsedState(pkgCollapsed);
  }

  getPackageCollapsedState() {
    let packageCollapsedState = {};
    const pkgCollapsedSessionVal = sessionStorage.getItem('stagingPkgCollapsedState');
    if (pkgCollapsedSessionVal) {
      packageCollapsedState = JSON.parse(pkgCollapsedSessionVal);
    }
    return packageCollapsedState;
  }

  setPackageCollapsedState(pkgCollapsed) {
    if (pkgCollapsed) {
      sessionStorage.setItem('stagingPkgCollapsedState', JSON.stringify(pkgCollapsed));
    }
    else {
      sessionStorage.removeItem('stagingPkgCollapsedState');
    }
    this.setState({
      stagedAssets: this.state.stagedAssets,
      selectedPackages: this.state.selectedPackages,
      selectedAssets: this.state.selectedAssets,
      allSelected: this.state.allSelected,
      foundAssets: this.state.foundAssets
    });
  }

  isAssetSelected(asset) {
    return this.state.selectedAssets.indexOf(asset) >= 0;
  }

  toggleAsset(asset) {
    const selectedAssets = [...this.state.selectedAssets];
    const idx = selectedAssets.indexOf(asset);
    const selectedPackages = [...this.state.selectedPackages];
    let allSelected = this.state.allSelected;
    if (idx >= 0) {
      selectedAssets.splice(idx, 1);
      const pkg = asset.substring(0, asset.indexOf('/'));
      const pkgIdx = selectedPackages.indexOf(pkg);
      if (pkgIdx >= 0) {
        selectedPackages.splice(pkgIdx, 1);
      }
      allSelected = false;
    }
    else {
      selectedAssets.push(asset);
    }
    this.setState({
      stagedAssets: this.state.stagedAssets,
      selectedPackages: selectedPackages,
      selectedAssets: selectedAssets,
      allSelected: allSelected,
      foundAssets: []
    });
  }

  isPackageSelected(pkg) {
    return this.state.selectedPackages.indexOf(pkg) >= 0;
  }

  togglePackage(pkg) {
    const selectedPackages = [...this.state.selectedPackages];
    const idx = selectedPackages.indexOf(pkg);
    const selectedAssets = this.state.selectedAssets.filter(asset => !asset.startsWith(pkg + '/'));
    let allSelected = this.state.allSelected;

    if (idx >= 0) {
      selectedPackages.splice(idx, 1);
      allSelected = false;
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
      selectedAssets: selectedAssets,
      allSelected: allSelected,
      foundAssets: []
    });
  }

  toggleAllSelect() {
    return new Promise(resolve => {
      if (this.state.allSelected) {
        this.setState({
          stagedAssets: this.state.stagedAssets,
          selectedPackages: [],
          selectedAssets: [],
          allSelected: false,
          foundAssets: []
        }, resolve());
      }
      else {
        const pkgs = Object.keys(this.state.stagedAssets);
        const assets = [];
        pkgs.forEach(pkg => this.state.stagedAssets[pkg].forEach(asset => assets.push(pkg + '/' + asset.name)));
        this.setState({
          stagedAssets: this.state.stagedAssets,
          selectedPackages: pkgs,
          selectedAssets: assets,
          allSelected: true,
          foundAssets: []
        }, resolve());  
      }
    });
  }
  
  handleStage(asset) {
    const requestObj = { assets: [asset.packageName + '/' + asset.name] };
    $mdwUi.clearMessage();
    $mdwUi.hubLoading(true);
    let ok = false;
    const url = this.context.serviceRoot + '/com/centurylink/mdw/staging/' + 
        this.props.stage.userCuid + '/assets';
    fetch(new Request(url, { 
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'mdw-app-id': 'mdw-hub' },
      body: JSON.stringify(requestObj),
      credentials: 'same-origin'
    }))
    .then(response => {
      ok = response.ok;
      return response.json();
    })
    .then(json => {
      if (!ok) {
        $mdwUi.showMessage(json.status.message);
        $mdwUi.hubLoading(false);
      }
      else {
        location = this.context.hubRoot + '/staging/' + this.props.stage.userCuid;
      }
    });
  }

  anySelectedAssetHasChanges() {
    for (let pkg in this.state.stagedAssets) {
      for (let i in this.state.stagedAssets[pkg]) {
        let asset = this.state.stagedAssets[pkg][i];
        if (this.state.selectedAssets.indexOf(pkg + '/' + asset.name) >= 0) {
          if (asset.vcsDiffType) {
            return true;
          }
        } 
      }
    }
  }

  handleUnstage() {
    if (this.state.selectedAssets.length > 0) {
      if (this.anySelectedAssetHasChanges()) {
        this.confirmUnstageDialog.current.open('Selected asset(s) have changes.  Unstage?');
      }
      else {
        this.doUnstage();
      }
    }
  }

  handleConfirmUnstage(result) {
    if (result) {
      this.doUnstage(result);
    }
  }

  doUnstage() {
    if (this.state.selectedAssets.length > 0) {
      let assetPaths = '%5B' + this.state.selectedAssets.join(',') + '%5D';
      const url = this.context.serviceRoot + '/com/centurylink/mdw/staging/' + 
          this.props.stage.userCuid + '/assets?assetPaths=' + assetPaths;
      $mdwUi.clearMessage();
      $mdwUi.hubLoading(true);
      let ok = false;
      fetch(new Request(url, {
        method: 'DELETE',
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
          location = this.context.hubRoot + '/staging/' + this.props.stage.userCuid;
        }
        else {
          $mdwUi.showMessage(json.status.message);
        }
      });    
    }
  }

  handlePromote() {
    const message = 'All changes from ' + this.props.stage.branch.name + ' branch will be merged into ' + $mdwGitBranch + '.';
    if (!this.state.allSelected) {
      this.toggleAllSelect()
      .then(() => {
        this.confirmPromoteDialog.current.open(message);
      });
    }
    else {
      this.confirmPromoteDialog.current.open(message);
    }
  }
  
  handleConfirmPromote(entered) {
    if (entered) {
      this.doPromote(entered);
    }
  }

  doPromote(message) {
    const url = this.context.serviceRoot + '/com/centurylink/mdw/staging/' + 
        this.props.stage.userCuid + '/assets';
    $mdwUi.clearMessage();
    $mdwUi.hubLoading(true);
    let ok = false;
    fetch(new Request(url, {
      method: 'PATCH',
      headers: { Accept: 'application/json', 'mdw-app-id': 'mdw-hub' },
      credentials: 'same-origin',
      body: '{ "comment": "' + message + '" }'
    }))
    .then(response => {
      $mdwUi.hubLoading(false);
      ok = response.ok;
      return response.json();
    })
    .then(json => {
      if (ok) {
        location = this.context.hubRoot + '/staging/' + this.props.stage.userCuid;
      }
      else {
        $mdwUi.showMessage(json.status.message);
      }
    });
  }

  findAssets(input) {
    this.assetsLoading = true;
    var url = this.context.serviceRoot + '/Assets?find=' + input;
    fetch(new Request(url, {
      method: 'GET',
      headers: { Accept: 'application/json', 'mdw-app-id': 'mdw-hub' },
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(data => {
      this.setState({
        stagedAssets: this.state.stagedAssets,
        selectedPackages: this.state.selectedPackages,
        selectedAssets: this.state.selectedAssets,
        foundAssets: data.assets,
      }, () => this.assetsLoading = false);
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
          selectedAssets: [],
          foundAssets: []
        });
      }
      else {
        $mdwUi.showMessage(json.status.message);
      }
    });
  }

  renderAssetMenu(items, menuProps) {
    return (
      <Menu {...menuProps}>
        {
          items.map((item, i) => {
            return (
              <li key={i} option={item} position={i}>
                <a className="dropdown-item" style={{cursor:'pointer'}}
                  onClick={e => { e.preventDefault; this.handleStage(item); }} >
                  <Highlighter search={menuProps.text}>
                    {item.match}
                  </Highlighter>
                  {item.match.indexOf('/') === -1 &&
                    <span>
                      {' (' + item.packageName + ')'}
                    </span>
                  }
                </a>
            </li>
            );
          })
        }
      </Menu>
    );
  }

  render() {
    const pkgs = this.state.stagedAssets ? Object.keys(this.state.stagedAssets) : undefined;
    const cuid = this.props.stage.userCuid;

    return (
      <div>
        <div className="panel-heading mdw-heading" style={{borderColor:'#ddd'}}>
          <div className="mdw-heading-label">
            <input type="checkbox" style={{marginRight:'6px'}}
              checked={this.state.allSelected} 
              onChange={() => this.toggleAllSelect()} />
            Staged Assets for {this.props.stage.userName}
            {' '}
            <a href={this.context.hubRoot + '/staging'} 
              onClick={e => {e.preventDefault(); this.collapseAll(); }}>
              <Glyphicon className="mdw-action-icon button button-primary" glyph="chevron-up" />
            </a>
            {' '}
            <a href={this.context.hubRoot + '/staging'}
              onClick={e => {e.preventDefault(); this.expandAll(); }}>
              <Glyphicon className="mdw-action-icon button button-primary" glyph="chevron-down" />
            </a>
          </div>
          <AsyncTypeahead className="mdw-typeahead"
            placeholder="Find an asset..."
            clearButton
            onSearch={this.findAssets} 
            options={this.state.foundAssets}
            labelKey="match"
            isLoading={this.assetsLoading || false}
            renderMenu={this.renderAssetMenu} 
            onChange={this.handleStage} />
          <div style={{float:'right'}}>
            <HelpButton title="Staging Help"
              url={this.context.hubRoot + '#/asset/com.centurylink.mdw.staging/readme.md'} />
            {this.context.authUser.workgroups.includes('Site Admin') &&
              <StagesPopButton />
            }
            <NewAsset stagingCuid={cuid} />
            <Button className="btn btn-primary mdw-btn mdw-action-btn"
              title="Unstage Assets" 
              disabled={!this.state.stagedAssets || Object.keys(this.state.stagedAssets).length === 0} 
              onClick={this.handleUnstage}>
              <Glyphicon glyph="arrow-left" />
              {' Unstage'}
            </Button>
            <Button className="btn btn-primary mdw-btn mdw-action-btn"
              title="Promote Assets" 
              disabled={!this.state.stagedAssets || Object.keys(this.state.stagedAssets).length === 0}
              onClick={this.handlePromote}>
              <Glyphicon glyph="arrow-right" />
              {' Promote'}
            </Button>
            <Confirm title="Unstage Assets" 
              ref={this.confirmUnstageDialog} 
              onClose={this.handleConfirmUnstage} />
            <Enter title="Promote Assets" 
              label="Commit Message: "
              ref={this.confirmPromoteDialog} 
              onClose={this.handleConfirmPromote} />
          </div>
        </div>
        <div className="mdw-section" style={{padding:'0'}}>
          <div style={{minHeight:'480px'}}>
            {this.state.stagedAssets &&
              <div>
                {pkgs.length === 0 &&
                  <div style={{padding:'10px'}}>
                    To add an asset to your staging area, search for it above, or browse 
                    under <a href={this.context.hubRoot + '#/packages'}>Assets</a>, 
                    and then click the Stage button.
                  </div>
                }
                {pkgs.length > 0 &&
                  <div>
                    {
                      pkgs.map((pkg, i) => {
                        let collapsed = this.isCollapsed(pkg);
                        return (
                          <div className="mdw-sub" key={i}>
                            <div className="panel-heading mdw-sub-heading">
                              <div className="mdw-heading-checklist">
                                <input type="checkbox" style={{top:'0'}} 
                                  checked={this.isPackageSelected(pkg)}
                                  onChange={() => this.togglePackage(pkg)} />
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
                                        onChange={() => this.toggleAsset(pkg + '/' + asset.name)} />
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
            }
            {!this.state.stagedAssets &&
              <div></div>
            }
          </div>
        </div>
      </div>
    );
  }
}

UserStage.contextType = MdwContext;
UserStage.propTypes = { stagingCuid: PropTypes.string };
export default UserStage; 