package com.cyberesicg.oscal_cprt.gui;

import com.cyberesicg.oscal_cprt.logic.AppUtils;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.util.prefs.Preferences;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

/**
 *
 * @author jimlh
 */
public class PrefsDialog extends javax.swing.JPanel {
    static JDialog dlg = null;

    /**
     * Creates new form PrefsDialog
     */
    public PrefsDialog() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        outFolderLbl = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        outFolderBtn = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        DoneBtn = new javax.swing.JButton();

        setMaximumSize(new java.awt.Dimension(500, 400));
        setMinimumSize(new java.awt.Dimension(300, 60));
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));

        jPanel2.setMaximumSize(new java.awt.Dimension(32827, 20));
        jPanel2.setMinimumSize(new java.awt.Dimension(160, 20));
        jPanel2.setPreferredSize(new java.awt.Dimension(400, 20));
        jPanel2.setRequestFocusEnabled(false);
        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.LINE_AXIS));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel1.setText("Preferences");
        jLabel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4));
        jLabel1.setMaximumSize(new java.awt.Dimension(80, 50));
        jLabel1.setMinimumSize(new java.awt.Dimension(80, 50));
        jPanel2.add(jLabel1);

        jPanel3.setMaximumSize(new java.awt.Dimension(32767, 20));
        jPanel3.setMinimumSize(new java.awt.Dimension(10, 20));
        jPanel3.setName(""); // NOI18N
        jPanel3.setPreferredSize(new java.awt.Dimension(277, 277));
        jPanel3.setRequestFocusEnabled(false);
        jPanel3.setLayout(null);
        jPanel2.add(jPanel3);

        add(jPanel2);

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 10, 1, 10));
        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.X_AXIS));

        jLabel2.setText("Output Folder: ");
        jLabel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 5));
        jPanel1.add(jLabel2);

        outFolderLbl.setBackground(new java.awt.Color(255, 255, 255));
        outFolderLbl.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        outFolderLbl.setText(".");
        outFolderLbl.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(50, 50, 50)), javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 5)));
        outFolderLbl.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        outFolderLbl.setMaximumSize(new java.awt.Dimension(300, 16));
        outFolderLbl.setMinimumSize(new java.awt.Dimension(100, 20));
        outFolderLbl.setPreferredSize(new java.awt.Dimension(200, 20));
        jPanel1.add(outFolderLbl);

        jPanel4.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));
        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.LINE_AXIS));

        outFolderBtn.setText("Change");
        outFolderBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                outFolderBtnActionPerformed(evt);
            }
        });
        jPanel4.add(outFolderBtn);

        jPanel1.add(jPanel4);

        add(jPanel1);

        jPanel5.setMaximumSize(new java.awt.Dimension(32767, 32767));
        jPanel5.setLayout(new javax.swing.BoxLayout(jPanel5, javax.swing.BoxLayout.LINE_AXIS));
        add(jPanel5);

        jPanel6.setMaximumSize(new java.awt.Dimension(32767, 20));
        jPanel6.setMinimumSize(new java.awt.Dimension(1, 20));
        jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6, javax.swing.BoxLayout.LINE_AXIS));

        jPanel7.setMaximumSize(new java.awt.Dimension(32767, 20));
        jPanel7.setMinimumSize(new java.awt.Dimension(0, 20));

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 297, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        jPanel6.add(jPanel7);

        jPanel8.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 5, 5));
        jPanel8.setLayout(new javax.swing.BoxLayout(jPanel8, javax.swing.BoxLayout.LINE_AXIS));

        DoneBtn.setText("Done");
        DoneBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DoneBtnActionPerformed(evt);
            }
        });
        jPanel8.add(DoneBtn);

        jPanel6.add(jPanel8);

        add(jPanel6);
    }// </editor-fold>//GEN-END:initComponents

    private void outFolderBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_outFolderBtnActionPerformed
            JFileChooser fileChooser = new JFileChooser(outFolderLbl.getText());
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = fileChooser.showOpenDialog(this);
            if(option == JFileChooser.APPROVE_OPTION){
                File file = fileChooser.getSelectedFile();
                String absPath = file.getAbsolutePath();
                outFolderLbl.setText(absPath);
                AppUtils.gOutFolder = absPath;
                Preferences prefs = Preferences.userNodeForPackage(com.cyberesicg.oscal_cprt.logic.AppUtils.class);
                prefs.put("gOutFolder", AppUtils.gOutFolder);
                AppUtils.gMainFrame.getOutFolderLbl().setText(absPath);
            }else{
//               outFolderLbl.setText("."+File.separator);
            }
    }//GEN-LAST:event_outFolderBtnActionPerformed

    private void DoneBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DoneBtnActionPerformed
        JDialog lDlg = PrefsDialog.dlg;
        lDlg.setVisible(false); 
        lDlg.dispose(); 
    }//GEN-LAST:event_DoneBtnActionPerformed

    static void makeNewPrefsDlg(JFrame parent)
    {
        PrefsDialog pnl = new PrefsDialog();
        pnl.getOutFolderLbl().setText(AppUtils.gOutFolder);
        pnl.getOutFolderLbl().setToolTipText(AppUtils.gOutFolder);
        PrefsDialog.dlg = new JDialog(parent);
        JDialog lDlg = PrefsDialog.dlg;
        if(parent != null) {
            Dimension parentSize = parent.getSize(); 
            Point p = parent.getLocation(); 
            lDlg.setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
        }

        lDlg.getContentPane().add(pnl);
        lDlg.setSize(600, 200);
        lDlg.setMinimumSize(new Dimension(600, 200));
        lDlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        lDlg.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        lDlg.setVisible(true);
        
    }

    javax.swing.JLabel getOutFolderLbl(){
        return outFolderLbl;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton DoneBtn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JButton outFolderBtn;
    private javax.swing.JLabel outFolderLbl;
    // End of variables declaration//GEN-END:variables
}
