
package org.keycloak.testsuite.ui.account2.page.fragment;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

/**
 * Header page fragment shown to an authenticated user on the Account Console v2,
 * resolving desktop vs mobile controls for logout, locale switching, the referrer
 * link, and the mobile navigation kebab menu.
 */
public class LoggedInPageHeader extends AbstractHeader {

    @FindBy(id = "logout")
    private WebElement logoutBtn;

    @FindBy(id = "logout-mobile")
    private WebElement logoutBtnMobile;

    @FindBy(id = "locale")
    private WebElement localeBtn;

    @FindBy(id = "locale-mobile")
    private WebElement localeBtnMobile;

    @FindBy(xpath = "//div[@id='locale']//ul")
    private WebElement localeDropdown;

    @FindBy(xpath = "//div[@id='locale-mobile']//ul")
    private WebElement localeDropdownMobile;

    @FindBy(id = "referrer-link")
    private WebElement referrerLink;

    @FindBy(id = "referrer-link-mobile")
    private WebElement referrerLinkMobile;

    @FindBy(id = "mobile-kebab")
    private WebElement mobileKebab;

    @Override
    public void clickMobileKebab() {
        clickLink(mobileKebab);
    }

    @Override
    protected WebElement getLocaleBtn() {
        return isMobileLayout() ? localeBtnMobile : localeBtn;
    }

    @Override
    protected WebElement getLocaleDropdown() {
        return isMobileLayout() ? localeDropdownMobile : localeDropdown;
    }

    @Override
    protected WebElement getLogoutBtn() {
        return isMobileLayout() ? logoutBtnMobile : logoutBtn;
    }

    @Override
    protected WebElement getReferrerLink() {
        return isMobileLayout() ? referrerLinkMobile : referrerLink;
    }
}
