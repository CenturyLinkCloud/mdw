import React, {Component} from '../node/node_modules/react';
import MdwContext from '../react/MdwContext';
import HelpButton from '../react/HelpButton.jsx';
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
    this.retrieveUserStage = this.retrieveUserStage.bind(this);

    this.stageCuid = this.getStageCuid();

    this.state = { 
      stage: {}
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
        headers: { 'Content-Type': 'application/json', 'mdw-app-id': 'mdw-hub' },
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
        headers: { Accept: 'application/json', 'mdw-app-id': 'mdw-hub' },
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

  render() {
    const userName = this.state.stage ? this.state.stage.userName : undefined;
    const stagingBranch = this.state.stage ? this.state.stage.branch : undefined;
    const isStagePrepared = this.state.stage && this.state.stage.prepared;
    return (
      <div>
        {isStagePrepared &&
          <UserStage stage={this.state.stage} />        
        }
        {!isStagePrepared &&
          <div>
            <div className="panel-heading mdw-heading" style={{borderColor:'#ddd'}}>
              <div className="mdw-heading-label">
                Staged Assets {userName ? ' for ' + userName : ''}
              </div>
              <div style={{float:'right'}}>
                <HelpButton title="Staging Help"
                  url={this.context.hubRoot + '#/asset/com.centurylink.mdw.staging/readme.md'} />
                {this.context.authUser.workgroups.includes('Site Admin') &&
                  <StagesPopButton />
                }
              </div>
            </div>
            <div className="mdw-section" style={{padding:'0'}}>
              <div style={{minHeight:'480px'}}>
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
        }
      </div>
    );
  }
}

Stage.contextType = MdwContext;
export default Stage; 