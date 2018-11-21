package eu.isas.peptideshaker.gui.pride;

import java.awt.event.KeyEvent;

/**
 * A dialog for getting the PRIDE private project access information from the
 * user.
 *
 * @author Harald Barsnes
 */
public class PridePrivateDataDialog extends javax.swing.JDialog {

    /**
     * The project accession.
     */
    private String projectAccession = null;
    /**
     * The user name.
     */
    private String userName = null;
    /**
     * The password.
     */
    private String password = null;

    /**
     * Creates a new PridePrivateDataDialog.
     *
     * @param parent the parent frame
     * @param modal if the dialog is to be modal or not
     * @param pxAccesion the PX accession to open, can be null
     */
    public PridePrivateDataDialog(java.awt.Frame parent, boolean modal, String pxAccesion) {
        super(parent, modal);
        initComponents();
        if (pxAccesion != null) {
            accessionTextField.setText(pxAccesion);
            userNameTextField.requestFocus();
        }
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    /**
     * Returns the project accession.
     *
     * @return the project accession
     */
    public String getProjectAccession() {
        return projectAccession;
    }

    /**
     * Returns the user name.
     *
     * @return the user name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Returns the password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        backgroundPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        projectPanel = new javax.swing.JPanel();
        accessionTextField = new javax.swing.JTextField();
        accessionLabel = new javax.swing.JLabel();
        userDetailsPanel = new javax.swing.JPanel();
        userNameLabel = new javax.swing.JLabel();
        userNameTextField = new javax.swing.JTextField();
        passwordLabel = new javax.swing.JLabel();
        passwordField = new javax.swing.JPasswordField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Private Data");
        setResizable(false);

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        okButton.setText("OK");
        okButton.setEnabled(false);
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        projectPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Project Details"));
        projectPanel.setOpaque(false);

        accessionTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        accessionTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                accessionTextFieldKeyReleased(evt);
            }
        });

        accessionLabel.setText("Accession");

        javax.swing.GroupLayout projectPanelLayout = new javax.swing.GroupLayout(projectPanel);
        projectPanel.setLayout(projectPanelLayout);
        projectPanelLayout.setHorizontalGroup(
            projectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, projectPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(accessionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(accessionTextField)
                .addContainerGap())
        );
        projectPanelLayout.setVerticalGroup(
            projectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(projectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(accessionTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(accessionLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        userDetailsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("User Details"));
        userDetailsPanel.setOpaque(false);

        userNameLabel.setText("User Name");

        userNameTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        userNameTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                userNameTextFieldKeyReleased(evt);
            }
        });

        passwordLabel.setText("Password");

        passwordField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        passwordField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                passwordFieldKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout userDetailsPanelLayout = new javax.swing.GroupLayout(userDetailsPanel);
        userDetailsPanel.setLayout(userDetailsPanelLayout);
        userDetailsPanelLayout.setHorizontalGroup(
            userDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(userDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(userDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(userDetailsPanelLayout.createSequentialGroup()
                        .addComponent(userNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(userNameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 363, Short.MAX_VALUE))
                    .addGroup(userDetailsPanelLayout.createSequentialGroup()
                        .addComponent(passwordLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(passwordField)))
                .addContainerGap())
        );
        userDetailsPanelLayout.setVerticalGroup(
            userDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(userDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(userDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(userNameLabel)
                    .addComponent(userNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(userDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(passwordLabel)
                    .addComponent(passwordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(userDetailsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(projectPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(111, 111, 111)
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(projectPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(userDetailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(okButton)
                    .addComponent(cancelButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Close the dialog and discard the input.
     *
     * @param evt
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        projectAccession = null;
        userName = null;
        password = null;
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Save the selected input and then close the dialog.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        projectAccession = accessionTextField.getText().trim();
        userName = userNameTextField.getText().trim();

        password = "";
        for (int i = 0; i < passwordField.getPassword().length; i++) {
            password += passwordField.getPassword()[i];
        }
        password = password.trim();

        dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Validate the input.
     *
     * @param evt
     */
    private void accessionTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_accessionTextFieldKeyReleased
        if (validateInput()) {
            if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                okButtonActionPerformed(null);
            }
        }
    }//GEN-LAST:event_accessionTextFieldKeyReleased

    /**
     * Validate the input.
     *
     * @param evt
     */
    private void userNameTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_userNameTextFieldKeyReleased
        accessionTextFieldKeyReleased(evt);
    }//GEN-LAST:event_userNameTextFieldKeyReleased

    /**
     * Validate the input.
     *
     * @param evt
     */
    private void passwordFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_passwordFieldKeyReleased
        accessionTextFieldKeyReleased(evt);
    }//GEN-LAST:event_passwordFieldKeyReleased

    /**
     * Validate the input and enable/disable the OK button.
     */
    private boolean validateInput() {

        boolean validated = !accessionTextField.getText().isEmpty()
                && !userNameTextField.getText().isEmpty()
                && passwordField.getPassword().length > 0;

        okButton.setEnabled(validated);
        return validated;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel accessionLabel;
    private javax.swing.JTextField accessionTextField;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton okButton;
    private javax.swing.JPasswordField passwordField;
    private javax.swing.JLabel passwordLabel;
    private javax.swing.JPanel projectPanel;
    private javax.swing.JPanel userDetailsPanel;
    private javax.swing.JLabel userNameLabel;
    private javax.swing.JTextField userNameTextField;
    // End of variables declaration//GEN-END:variables
}
