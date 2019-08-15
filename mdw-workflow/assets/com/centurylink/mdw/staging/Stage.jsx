import React, {Component} from '../node/node_modules/react';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import MdwContext from '../react/MdwContext';
import Progress from '../react/Progress.jsx';
import StagesPopButton from './StagesPopButton.jsx';
import NoStage from './NoStage.jsx';
import UserStage from './UserStage.jsx';

class Stage extends Component {
    
  constructor(...args) {
    super(...args);

    this.getStageCuid = this.getStageCuid.bind(this);  
    this.prepareStagingArea = this.prepareStagingArea.bind(this);  
    this.handleProgressStart = this.handleProgressStart.bind(this);
    this.handleProgressFinish = this.handleProgressFinish.bind(this);
    this.handleProgressError = this.handleProgressError.bind(this);
    this.getPackageCollapsedState = this.getPackageCollapsedState.bind(this);
    this.setPackageCollapsedState = this.setPackageCollapsedState.bind(this);
    this.handleCollapseAll = this.handleCollapseAll.bind(this);
    this.handleExpandAll = this.handleExpandAll.bind(this);
    this.handleCollapse = this.handleCollapse.bind(this);
    this.handleExpand = this.handleExpand.bind(this);
    this.retrieveUserStage = this.retrieveUserStage.bind(this);
    this.createPackage = this.createPackage.bind(this);

    this.stageCuid = this.getStageCuid();

    this.state = { 
      stage: {},
      packageCollapsedState: this.getPackageCollapsedState()
    };
  }

  /**
   * Returns cuid from path.
   */
  getStageCuid() {
    var stageCuid = undefined;
    if (this.props.match && this.props.match.params && this.props.match.params.cuid) {
      stageCuid = this.props.match.params.cuid;
    }
    else if (this.props.location && this.props.location.hash && this.props.location.hash.startsWith('#/staging/')) {
      stageCuid = this.props.location.hash.substring(10);
    }
    return stageCuid;
  }

  /**
   * Returns a promise that resolves to true if staging area already prepared.
   */
  prepareStagingArea(cuid) {
    $mdwUi.clearMessage();
    return new Promise(resolve => {
      const url = this.context.serviceRoot + '/com/centurylink/mdw/staging/' + cuid;
      var status = 0;
      fetch(new Request(url, {
        method: 'POST',
        headers: { Accept: 'application/json'},
        body: '{}',
        credentials: 'same-origin'
      }))
      .then(response => {
        status = response.status;
        return response.json();
      })
      .then(json => {
        if (status === 200 || status === 202) {
          const stage = json;
          stage.prepared = status === 200;
          resolve(stage);
        }
        else {
          $mdwUi.showMessage(json.status.message);
          resolve();
        }
      });
    });
  }

  handleProgressStart() {
  }

  handleProgressFinish() {
    setTimeout(() => {
      this.setState({
        stage: Object.assign(this.state.stage, { prepared: true})
      });  
    }, 500);
  }

  handleProgressError(message) {
    $mdwUi.showMessage(message);
  }

  /**
   * Promise resolves with user stage if found.
   */
  retrieveUserStage(cuid) {
    return new Promise(resolve => {
      const url = this.context.serviceRoot + '/com/centurylink/mdw/staging/' + cuid;
      $mdwUi.clearMessage();
      var status = 0;
      fetch(new Request(url, {
        method: 'GET',
        headers: { Accept: 'application/json'},
        credentials: 'same-origin'
      }))
      .then(response => {
        status = response.status;
        return response.json();
      })
      .then(json => {
        if (status === 200) {
          resolve(json);
        }
        else if (status == 404) {
          resolve();
        }
        else {
          $mdwUi.showMessage(json.status.message);
          resolve();
        }
      });
    });
  }

  componentDidMount() {
    // when user is included in the path this means prepare staging area
    if (this.stageCuid) {
      $mdwUi.hubLoading(true);
      this.prepareStagingArea(this.stageCuid)
      .then(stage => {
        $mdwUi.hubLoading(false);
        this.setState({
          stage: stage
        });
      });
    }
  }

  componentDidUpdate() {
    if (this.context.authUser.cuid && !this.stageCuid) {
      // set stageCuid to current authUser
      this.stageCuid = this.context.authUser.cuid;
      $mdwUi.hubLoading(true);
      this.retrieveUserStage(this.stageCuid)
      .then(userStage => {
        if (userStage) {
          this.prepareStagingArea(this.stageCuid)
          .then(stage => {
            $mdwUi.hubLoading(false);
            this.setState({
              stage: stage
            });
          });
        }
        else {
          $mdwUi.hubLoading(false);
          // stage not found
          this.setState({
            stage: undefined
          });
        }
      });
    }
  }

  createPackage() {
    // console.log("create package");
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
      stage: this.state.stage,
      packageCollapsedState: pkgCollapsed
    });
  }

  handleCollapse(pkg) {
    let pkgCollapsed = this.getPackageCollapsedState();
    pkgCollapsed[pkg] = true;
    this.setPackageCollapsedState(pkgCollapsed);
  }
  handleExpand(pkg) {
    let pkgCollapsed = this.getPackageCollapsedState();
    pkgCollapsed[pkg] = false;
    this.setPackageCollapsedState(pkgCollapsed);
  }
  handleCollapseAll() {
    let pkgCollapsed = this.getPackageCollapsedState();
    Object.keys(pkgCollapsed).forEach(pkgName => {
      pkgCollapsed[pkgName] = true;
    });
    this.setPackageCollapsedState(pkgCollapsed);
  }
  handleExpandAll() {
    let pkgCollapsed = this.getPackageCollapsedState();
    Object.keys(pkgCollapsed).forEach(pkgName => {
      pkgCollapsed[pkgName] = false;
    });
    this.setPackageCollapsedState(pkgCollapsed);
  }

  render() {
    sessionStorage.setItem('stagingUser', this.stageCuid);
    const userName = this.state.stage ? this.state.stage.userName : undefined;
    const stagingBranch = this.state.stage ? this.state.stage.branch : undefined;
    const isStagePrepared = this.state.stage && this.state.stage.prepared;
    return (
      <div>
        <div className="panel-heading mdw-heading" style={{borderColor:'#ddd'}}>
          <div className="mdw-heading-label">
            Staged Assets {userName ? ' for ' + userName : ''}
            {isStagePrepared &&
              <span>
                {' '}
                <a href={this.context.hubRoot + '/staging'} 
                  onClick={e => {e.preventDefault(); this.handleCollapseAll(); }}>
                  <Glyphicon className="mdw-action-icon button button-primary" glyph="chevron-up" />
                </a>
                {' '}
                <a href={this.context.hubRoot + '/staging'}
                  onClick={e => {e.preventDefault(); this.handleExpandAll(); }}>
                  <Glyphicon className="mdw-action-icon button button-primary" glyph="chevron-down" />
                </a>
              </span>
            }
          </div>
          <div style={{float:'right'}}>
            {isStagePrepared &&
              <Button className="btn btn-primary mdw-btn mdw-action-btn"
                title="New Package" onClick={this.createPackage}>
                <Glyphicon glyph="plus" />
              </Button>             
            }
            {this.context.authUser.workgroups.includes('Site Admin') &&
              <StagesPopButton />
            }
          </div>
        </div>
        <div className="mdw-section" style={{padding:'0'}}>
          <div style={{minHeight:'480px'}}>
            {isStagePrepared &&
              <UserStage stage={this.state.stage} 
                packageCollapse={this.state.packageCollapsedState}
                onExpand={this.handleExpand} 
                onCollapse={this.handleCollapse}/>
            }
            {this.stageCuid && !this.state.stage &&
              <div style={{padding:'10px'}}>
                <NoStage />
              </div>
            }
            {this.stageCuid && !isStagePrepared && stagingBranch && $mdwWebSocketUrl &&
              <div style={{padding:'10px'}}>
                <Progress
                  title={'Prepare staging area for ' + this.state.stage.userName}
                  webSocketUrl={$mdwWebSocketUrl}
                  topic={stagingBranch.name}
                  onStart={this.handleProgressStart}
                  onFinish={this.handleProgressFinish}
                  onError={this.handleProgressError} />
              </div>
            }
          </div>
        </div>
      </div>
    );
  }
}

Stage.contextType = MdwContext;
export default Stage; 