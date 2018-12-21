import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';

// Header component to fit in with standard MDWHub look-and-feel.
class Header extends Component {
  constructor(...args) {
    super(...args);
    this.state = {
      tabs: [
        {
          index: 0,
          label: 'Processes',
          url: this.context.hubRoot + '/#/workflow/processes',
          classes: 'mdw_tab_inactive mdw_tab_first',
          active: false
        },
        {
          index: 1,
          label: 'Dashboard',
          url: this.context.hubRoot + '/dashboard/processes',
          classes: 'mdw_tab_active',
          active: true
        },
        {
          index: 2,
          label: 'Services',
          url: this.context.hubRoot + '/#/serviceApi',
          classes: 'mdw_tab_inactive mdw_tab_last',
          active: false
        }
      ]
    };
  }

  render() {
    return(
      <div>
        <div id="mdw-header" className="mdw-normalize">
          <div className="mdw_banner">
            <div className="mdw_bannerLeft">
             <img src={this.context.hubRoot + '/images/mdw.png'} alt="MDW" style={{cursor:'pointer'}} onClick={() => window.location.href = this.context.hubRoot} />
             <img src={this.context.hubRoot + '/images/hub.png'} alt="Hub" style={{marginLeft:'1px',cursor:'pointer'}} onClick={() => window.location.href = this.context.hubRoot} />
             <img id="hub_logo" src={this.context.hubRoot + '/images/hub_logo.png'} alt="Hub Logo" style={{marginLeft:'1px'}} />
             <img id="hub_loading" src={this.context.hubRoot + '/images/hub_loading.gif'} alt="Hub Loading" style={{display:'none',marginLeft:'1px'}} />
            </div>
            <div style={{float:'left'}}>
             <div id="mdwMainMessages" className="mdw-messages"></div>
            </div>
            <div className="mdw_bannerRight">
             <img src={this.context.hubRoot + '/images/ctl.gif'} alt="CenturyLink(R)" />
            </div>
          </div>
          <div className="mdw_user mdw-normalize" style={{display:'table'}}>
            <div style={{display:'table-row'}}>
              <div className="mdw-tabs-space" style={{display:'table-cell'}}>
              </div>
              <div className="mdw_session" style={{display:'table-cell',width:'1px'}}>
                <a href={this.context.hubRoot + '#/users/' + this.context.authUser.cuid}>
                  <img src={this.context.hubRoot + '/images/user.png'} alt={this.context.authUser.name} className="mdw_userIcon"/><span className="mdw_welcomeText">{this.context.authUser.name}</span>
                </a>
              </div>
              <div className="mdw_logout" style={{display:'table-cell'}}>
                {this.context.authUser.cuid &&
                  <a href={this.context.hubRoot + '/logout'} className="mdw_actionLink mdw_welcomeLink">Sign Out</a>
                }
                {!this.context.authUser.cuid &&
                  <a href={this.context.hubRoot + '/login'} className="mdw_actionLink mdw_welcomeLink">Sign In</a>
                }
              </div>
            </div>
          </div>
        </div>
        <div id="mdw-tabs" className="mdw-normalize">
          <table id="tabPanel" className="mdw_tabpanel" cellPadding="0">
            <tbody>
              <tr>
              {
                this.state.tabs.map(tab => {
                  return (
                    <td key={tab.index} className={tab.classes}>
                      <div className="mdw_tab">
                        <div>
                          <a tabIndex="{tab.index}" href="{tab.url}">{tab.label}</a>
                        </div>
                        {tab.active &&
                          <img className="mdw_tab_active_image" src={this.context.hubRoot + '/images/tab_sel.png'}></img>
                        }
                      </div>
                      {tab.index < this.state.tabs.length &&
                        <img src={this.context.hubRoot + '/images/tab_spacer.png'}></img>
                      }
                    </td>
                  );
                })
              }
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    );
  }
}

Header.contextTypes = {
  hubRoot: PropTypes.string,
  authUser: PropTypes.object
};

export default Header;
