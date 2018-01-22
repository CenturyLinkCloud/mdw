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
    items.forEach(item=> {
      var size = item.size ? item.size.toString() : '';
      if (!dirItem.permissions) {
        // windows size format contains commas
        size = size.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
      }
      if (size.length > longestSize) {
        longestSize = size.length;
      }
      sizes.push(size);
    });
    
    console.log("SIZES: " + JSON.stringify(sizes, null, 2));
    
    var lines = [];
    items.forEach((item, i) => {
      const mod = new Date(item.modified);
      if (dirItem.permissions) {
        // linux-style
        
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
  
export default DirListing;