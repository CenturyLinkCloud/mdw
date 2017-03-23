/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.designer.icons;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.Hashtable;

import javax.swing.ImageIcon;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.pages.DesignerPage;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class IconFactory {

    Hashtable<String,javax.swing.Icon> icons = null;

    private DesignerDataAccess dao;
    public void setDesignerDataAccess(DesignerDataAccess dao) {
        this.dao = dao;
    }

    public IconFactory() {
    }

    public IconFactory(DesignerDataAccess dao) {
        this.dao = dao;
    }

    public javax.swing.Icon getIcon(String name) {
        if (icons == null)
            icons = new Hashtable<String,javax.swing.Icon>();
        javax.swing.Icon icon = icons.get(name==null?"":name);
        if (icon==null) {
            if (name==null) {
                icon = new Icon();
                ((Icon)icon).errmsg = "Iconname is null";
                name = "";
            } else if (name.startsWith("shape:")) {
                icon = getShapeIcon(name);
            } else {
                int k = name.indexOf('@');
                if (k>0) name = name.substring(0,k);
                try {
                    byte imagebytes[] = loadIcon(name);
                    if (imagebytes!=null) {
                        icon = new ImageIcon(imagebytes);
                        ((ImageIcon)icon).setDescription(name);
                    } else {
                        icon = new Icon();
                        ((Icon)icon).errmsg = "Cannot load icon " + name;
                    }
                } catch (DataAccessException e) {
                    icon = new Icon();
                    ((Icon)icon).errmsg = "Data Access exception: " + e.getMessage();
                } catch (IOException e) {
                    icon = new Icon();
                    ((Icon)icon).errmsg = "I/O exception: " + e.getMessage();
                }

            }
            icons.put(name, icon);
        }
        return icon;
    }

    public javax.swing.Icon getIcon(String name, DesignerPage page) {
        javax.swing.Icon icon = getIcon(name);
        if (icon instanceof Icon) {
            String errmsg = ((Icon)icon).errmsg;
            if (errmsg!=null) page.showErrorDelayed(errmsg);
        }
        return icon;
    }

    private Icon getShapeIcon(String iconname) {
        Icon icon;
        if (iconname.equals("shape:start")) icon = new IconStart();
        else if (iconname.equals("shape:stop")) icon = new IconStop();
        else if (iconname.equals("shape:recvmsg")) icon = new IconRecvMsg();
        else if (iconname.equals("shape:sendmsg")) icon = new IconSendMsg();
        else if (iconname.equals("shape:task")) icon = new IconTask();
        else if (iconname.equals("shape:rule")) icon = new IconRule();
        else if (iconname.equals("shape:subproc")) icon = new IconEmbeddedProc();
        else if (iconname.equals("shape:invoke")) icon = new IconInvoke();
        else if (iconname.equals("shape:decision")) icon = new IconDecision();
        else if (iconname.equals("shape:activity")) icon = new IconJava();
        else {
            try {
                String clsname = iconname.substring(6);
                Class<?> cls = Class.forName(clsname);
                icon = (Icon)cls.newInstance();
            } catch (Exception e) {
                icon = null;
            }
            if (icon==null) {
                icon = new Icon();
                icon.errmsg = "Cannot load icon " + iconname;
            }
        }
        return icon;
    }

    private byte[] loadIcon(String file)
    throws RemoteException,IOException, DataAccessException {
        RuleSetVO ruleset = dao == null ? null : dao.getRuleSet(file, null, 0);
        byte[] bytes;
        if (ruleset!=null) {
            if (ruleset.isRaw()) {
                bytes = ruleset.getRawContent();
            }
            else {
                String content = ruleset.getRuleSet();
                int k = content.indexOf('\n');
                bytes = RuleSetVO.decode(content.substring(k+1));
            }
        } else {
            String resource = "/images/" + file;
            InputStream is = Icon.class.getResourceAsStream(resource);
            if (is==null) is = Icon.class.getResourceAsStream(file);
            if (is != null) {
                bytes = FileHelper.readFromResourceStream(is);
            } else bytes = null;
        }
        return bytes;
    }

    private ImageIcon scaleImageIcon(ImageIcon icon, int w, int h) {
        String iconname = icon.getDescription();
        int k = iconname.indexOf('@');
        if (k>0) {
            iconname = iconname.substring(0,k);
            icon = (ImageIcon)icons.get(iconname);
            if (icon.getIconWidth()==w && icon.getIconHeight()==h) return icon;
        }
        iconname = iconname + "@" + w + "x" + h;
        Image image = icon.getImage();
        icon = (ImageIcon)icons.get(iconname);
        if (icon==null) {
            image = image.getScaledInstance(w, h, Image.SCALE_DEFAULT);
            icon = new ImageIcon(image);
            icon.setDescription(iconname);
            icons.put(iconname, icon);
        }
        return icon;
    }

    public javax.swing.Icon scaleIcon(javax.swing.Icon icon, int w, int h) {
        if (icon instanceof ImageIcon) {
            return scaleImageIcon((ImageIcon)icon, w, h);
        } else {
            try {
                Icon orig = (Icon)icon;
                icon = orig.getClass().newInstance();
                ((Icon)icon).setIconWidth(w);
                ((Icon)icon).setIconHeight(h);
            } catch (Exception e) {
            }
        }
        return icon;
    }

    public javax.swing.Icon scaleIconToMax(javax.swing.Icon icon, int maxw, int maxh) {
        int h = icon.getIconHeight();
        int w = icon.getIconWidth();
        if (w>maxw || h>maxh) {
            double factor = Math.max((double)w/(double)maxw, (double)h/(double)maxh);
            w = (int)(w/factor);
            h = (int)(h/factor);
            icon = scaleIcon(icon, w, h);
        }
        return icon;
    }
}
