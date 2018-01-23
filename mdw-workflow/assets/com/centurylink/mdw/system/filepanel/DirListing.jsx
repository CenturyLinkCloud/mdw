import React from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import {Scrollbars} from '../../node/node_modules/react-custom-scrollbars';
// import {Scrollbars} from '../../../../../../../../react-custom-scrollbars';
import '../../node/node_modules/style-loader!./filepanel.css';

function DirListing(props) {

  const getDirListing = dirItem => {
    
    var items = [];
    if (dirItem.dirs) {
      dirItem.dirs.forEach(dir => {
        items.push(dir);
      });
    }
    if (dirItem.files) {
      dirItem.files.forEach(file => {
        file.isFile = true;
        items.push(file);
      });
    }
    items.sort((item1, item2) => {
      return new Date(item1.modified).getTime() - new Date(item2.modified).getTime();
    })
    
    var sizes = [];
    var longestSize = 0;
    var longestNumLinks = 0;
    var longestOwner = 0;
    var longestGroup = 0;
    items.forEach(item => {
      var size = item.size ? item.size.toString() : '';
      if (dirItem.permissions) {
        if (item.numLinks) {
          var numLinks = item.numLinks.toString();
          if (numLinks.length > longestNumLinks) {
            longestNumLinks = numLinks.length;
          }
        }
        if (item.owner && item.owner.length > longestOwner) {
          longestOwner = item.owner.length;
        }
        if (item.group && item.group.length > longestGroup) {
          longestGroup = item.group.length;
        }
      }
      else {
        // windows size format contains commas
        size = size.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
      }
      if (size.length > longestSize) {
        longestSize = size.length;
      }
      sizes.push(size);
    });
    
    var lines = [];
    var now = new Date();
    items.forEach((item, i) => {
      const mod = new Date(item.modified);
      if (dirItem.permissions) {
        // linux-style
        var line = (item.isFile ? '-' : 'd') + item.permissions + ' ';
        var numLinks = item.numLinks ? item.numLinks.toString() : (item.isFile ? '1' : '');
        line += numLinks.padStart(longestNumLinks) + ' ';
        line += (item.owner ? item.owner : '').padEnd(longestOwner) + ' ';
        line += (item.group ? item.group : '').padEnd(longestGroup) + ' ';
        line += sizes[i].padStart(longestSize) + ' ';
        line += DirListing.MONTHS[mod.getMonth()] + ' ';
        line += mod.getDate().toString().padStart(2) + ' ';
        if (mod.getFullYear() === now.getFullYear()) {
          line += mod.getHours().toString().padStart(2, '0') + ':' 
          line += mod.getMinutes().toString().padStart(2, '0') + ' ';
        }
        else {
          line += ' ' + mod.getFullYear() + ' ';
        }
        line += item.name;
      }
      else {
        // non-posix (windows)
        var line = (mod.getMonth() + 1).toString().padStart(2, '0') + '/' + 
              mod.getDate().toString().padStart(2, '0') + '/' + mod.getFullYear() + '  ';
        const hours = mod.getHours();
        if (hours === 0 || hours === 12) {
          line += '12:' + mod.getMinutes().toString().padStart(2, '0') + 
            (hours === 0 ? ' AM' : ' PM');
        }
        else if (hours > 12) {
          line += (hours - 12).toString().padStart(2, '0') + ':' + 
             mod.getMinutes().toString().padStart(2, '0') + ' PM';
        }
        else {
          line += hours.toString().padStart(2, '0') + ':' + 
             mod.getMinutes().toString().padStart(2, '0') + ' PM';
        }
        line += (item.isFile ? '           ' : '    <DIR>  ') + sizes[i].padStart(longestSize);
        line += ' ' + item.name;
      }
      lines.push(line);
    });
    return dirItem.path + '\n\n' + lines.join('\n');
  }
  
  return (
    <Scrollbars
      className="fp-scroll"
      thumbSize={30}>
      <div id="fp-dir-listing" className="fp-content">
        {getDirListing(props.item)}
      </div>
    </Scrollbars>
  );
}

DirListing.MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

export default DirListing;