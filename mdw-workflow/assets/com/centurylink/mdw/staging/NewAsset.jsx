import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, Glyphicon, Popover, OverlayTrigger, ControlLabel} from '../node/node_modules/react-bootstrap';
import MdwContext from '../react/MdwContext';
import Enter from '../react/Enter.jsx';

class NewAsset extends Component {

  constructor(...args) {
    super(...args);

    this.state = {
      packages: [],
      packageName: '',
      error: ''
    };

    this.newPackage = {
      name: '[New Package]...'
    };

    this.handlePackageNameChange = this.handlePackageNameChange.bind(this);
    this.createPackage = this.createPackage.bind(this);
    this.handleAssetNameChange = this.handleAssetNameChange.bind(this);

    this.handleNewAsset = this.handleNewAsset.bind(this);
    this.createAsset = this.createAsset.bind(this);

    this.entryDialog = React.createRef();
    this.packagesPop = React.createRef();
    this.newPackagePop = React.createRef();
  }

  handlePackageNameChange(event) {
    this.setState({
      packages: this.state.packages,
      packageName: event.currentTarget.value,
      error: ''
    });
  }

  createPackage(name) {
    const url = this.context.serviceRoot + '/Assets/' + name + '?stagingUser=' + this.props.stagingCuid;
    var ok = false;
    fetch(new Request(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'mdw-app-id': 'mdw-hub' },
      body: '{ "name": "' + name + '" }',
      credentials: 'same-origin'
    }))
    .then(response => {
      ok = response.ok;
      return response.json();
    })
    .then(json => {
      if (ok) {
        this.handleNewAsset(name);
        this.newPackagePop.current.hide();    
      }
      else {
        $mdwUi.showMessage(json.status.message);
      }
    });
  }

  handleAssetNameChange(name) {
    this.setState({
      packages: this.state.packages,
      packageName: this.state.packageName,
      error: name.indexOf('.') === -1 ? 'Asset name must have an extension' : ''
    });
  }

  handleNewAsset(packageName) {
    let msg = 'Create asset in ' + packageName + ':';
    this.setState({
      packages: this.state.packages,
      packageName: packageName,
      error: ''
    }, () => {
      this.entryDialog.current.open(msg);
      this.packagesPop.current.hide();
    });
  }

  createAsset(name) {
    if (name) {
      const url = this.context.serviceRoot + '/Assets/' + this.state.packageName + '/' + name + '?stagingUser=' + this.props.stagingCuid;
      var ok = false;
      fetch(new Request(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'mdw-app-id': 'mdw-hub' },
        body: '{ "name": "' + name + '" }',
        credentials: 'same-origin'
      }))
      .then(response => {
        ok = response.ok;
        return response.json();
      })
      .then(json => {
        if (ok) {
          location = this.context.hubRoot + '/staging/' + this.props.stagingCuid;
        }
        else {
          $mdwUi.showMessage(json.status.message);
        }
      });        
    }
  }

  componentDidMount() {
    var ok = false;
    const url = this.context.serviceRoot + '/Assets?packageList=true&withVcsInfo=false&stagingUser=' + this.props.stagingCuid;
    fetch(new Request(url, {
      method: 'GET',
      headers: { Accept: 'application/json', 'mdw-app-id': 'mdw-hub' },
      credentials: 'same-origin'
    }))
    .then(response => {
      ok = response.ok;
      return response.json();
    })
    .then(json => {
      if (ok) {
        this.setState({
          packages: [this.newPackage, ...json.packages],
          entity: this.state.entity,
          error: ''
        });
      }
    });
  }

  render() {

    const newPackagePopover = (
      <Popover id="new-package-pop" style={{width:'350px'}} placement="left">
        <div>
          <ControlLabel style={{marginRight:'5px'}}>
            Package Name:
          </ControlLabel>
          <input type="text" style={{width:'100%'}} autoFocus
            onChange={this.handlePackageNameChange} />
          <span style={{float:'right',marginTop:'5px'}}>
            <Button bsStyle="primary" className="mdw-btn" 
              onClick={() => this.createPackage(this.state.packageName)}>
              Create
            </Button>
            <Button className="mdw-btn" style={{marginLeft:'10px'}}
              onClick={() => {this.newPackagePop.current.hide(); this.packagesPop.current.hide();}}>
              Cancel
            </Button>
          </span>
        </div>
      </Popover>
    );

    const packagesPopover = (
      <Popover id="packages-pop" style={{width:'350px'}} placement="left">
        <ul className="dropdown-menu mdw-popover-menu">
        {
          this.state.packages.map((pkg, i) => {
            if (pkg.name === '[New Package]...') {
              return (
                <li key={i}>
                  <OverlayTrigger trigger="click"
                    ref={this.newPackagePop}
                    placement="left" 
                    overlay={newPackagePopover} 
                    rootClose={false}>
                    <a className="dropdown-item" style={{cursor:'pointer'}} >
                      {pkg.name}
                    </a>
                  </OverlayTrigger>
                </li>
              );
            }
            else {
              return (
                <li key={i}>
                  <a className="dropdown-item" style={{cursor:'pointer'}}
                    onClick={() => this.handleNewAsset(pkg.name)} >
                    {pkg.name}
                  </a>
              </li>
              );
            }
          })
        }          
        </ul>
      </Popover>
    );

    const {...popProps} = this.props; // eslint-disable-line no-unused-vars
    return (
      <div style={{display:'inline-block'}}>
        <OverlayTrigger trigger="click"
          ref={this.packagesPop} 
          placement="left"
          overlay={packagesPopover}
          rootClose={false}>
          <Button bsStyle="primary" className="mdw-btn" style={{marginLeft:'4px'}}
            title="Create New Asset">
            <Glyphicon glyph="plus" />
          </Button>
        </OverlayTrigger>
        <Enter title={'Create Asset'} 
          label="Name:"
          ref={this.entryDialog}
          onChange={this.handleAssetNameChange}
          onClose={this.createAsset}
          error={this.state.error} />
      </div>
    );
  }
}

NewAsset.contextType = MdwContext;
NewAsset.propTypes = { stagingCuid: PropTypes.string };
export default NewAsset;
