import React, {Component} from '../node/node_modules/react';
import {Popover, OverlayTrigger} from '../node/node_modules/react-bootstrap';
import MdwContext from '../react/MdwContext';
import HeaderPopButton from '../react/HeaderPopButton.jsx';
import Enter from '../react/Enter.jsx';

class NewAssetPackage extends Component {

  constructor(...args) {
    super(...args);

    this.state = {
      packages: [],
      entity: '',
      error: ''
    };

    this.handleChange = this.handleChange.bind(this);
    this.handleNew = this.handleNew.bind(this);
    this.handleCreate = this.handleCreate.bind(this);

    this.entryDialog = React.createRef();
    this.popButton = React.createRef();
  }

  handleChange(name) {
    if (this.state.entity === 'asset') {
      if (name.indexOf('.') === -1) {
        this.setState({
          packages: this.state.packages,
          entity: this.state.entity,
          error: 'Asset name must have an extension'
        });
      }
      else {
        this.setState({
          packages: this.state.packages,
          entity: this.state.entity,
          error: ''
        });
      }
    }
  }

  handleNew(entity, pkgName) {
    let msg = 'Enter name for new ' + entity;
    if (pkgName) {
      msg += ' in ' + pkgName;
    }
    this.setState({
      packages: this.state.packages,
      entity,
      error: ''
    }, this.entryDialog.current.open(msg));
  }

  handleCreate(name) {
    if (name && this.state.entity) {
      // console.log("create: " + this.state.entity + " '" + name + "'");
      this.popButton.current.hide();
    }
  }

  componentDidMount() {
    var ok = false;
    fetch(new Request(this.context.serviceRoot + '/Assets?packageList=true&withVcsInfo=false', {
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
          packages: json.packages,
          entity: this.state.entity,
          error: ''
        });
      }
    });
  }

  render() {
    const packagesPopover = (
      <Popover id="packages-pop" style={{width:'350px'}} placement="left">
        <ul className="dropdown-menu mdw-popover-menu">
        {
          this.state.packages.map((pkg, i) => {
            return (
              <li key={i}>
                <a className="dropdown-item" style={{cursor:'pointer'}}
                  onClick={() => this.handleNew('asset', pkg.name)} >
                  {pkg.name}
                </a>
            </li>
            );
          })
        }          
        </ul>
      </Popover>
    );

    const {...popProps} = this.props; // eslint-disable-line no-unused-vars
    return (
      <div style={{display:'inline-block'}}>
        <HeaderPopButton ref={this.popButton}
          glyph="plus"
          title="New Asset/Package"
          rootClose={true}
          popover={
            <Popover {...popProps} id="new-pop">
              <ul className="dropdown-menu mdw-popover-menu">
                <li>
                  <OverlayTrigger trigger="click"
                    placement="left" overlay={packagesPopover} rootClose={false}>
                    <a style={{cursor:'pointer'}}>
                      New Asset
                    </a>
                  </OverlayTrigger>
                </li>
                <li>
                  <a style={{cursor:'pointer'}}
                    onClick={() => this.handleNew('package')}>
                      New Package
                  </a>
                </li>
              </ul>
            </Popover>
          } 
        />
        <Enter title={'Create ' + this.state.entity} 
          label="Name:"
          ref={this.entryDialog}
          onChange={this.handleChange}
          onClose={this.handleCreate}
          error={this.state.error} />
      </div>
    );
  }
}

NewAssetPackage.contextType = MdwContext;
export default NewAssetPackage;
