package code.bankconnectors.akka

import java.lang
import java.util.Date

import code.api.util.{APIUtil, CallContextAkka}
import code.customer.{CreditLimit, CreditRating, Customer, CustomerFaceImage}
import code.model.dataAccess.MappedBankAccountData
import code.model.{AccountId, AccountRouting, AccountRule, BankAccount, BankId, BankIdAccountId, Bank => BankTrait}
import net.liftweb.mapper.By
import net.liftweb.util.Helpers.today

import scala.collection.immutable.List


/**
  *
  * case classes used to define outbound Akka messages
  *
  */
case class OutboundGetAdapterInfo(date: String, callContext: Option[CallContextAkka])
case class OutboundGetBanks(callContext: Option[CallContextAkka])
case class OutboundGetBank(bankId: String, callContext: Option[CallContextAkka])
case class OutboundCheckBankAccountExists(bankId: String, accountId: String, callContext: Option[CallContextAkka])
case class OutboundGetAccount(bankId: String, accountId: String, callContext: Option[CallContextAkka])
case class OutboundGetCoreBankAccounts(bankIdAccountIds: List[BankIdAccountId], callContext: Option[CallContextAkka])
case class OutboundGetCustomersByUserId(userId: String, callContext: Option[CallContextAkka])

/**
  *
  * case classes used to define inbound Akka messages
  *
  */
case class InboundAdapterInfo(
                               name: String,
                               version: String,
                               git_commit: String,
                               date: String,
                               callContext: Option[CallContextAkka]
                             )
case class InboundGetBanks(banks: Option[List[Bank]], callContext: Option[CallContextAkka])
case class InboundGetBank(bank: Option[Bank], callContext: Option[CallContextAkka])
case class InboundCheckBankAccountExists(data: Option[InboundAccountDec2018], callContext: Option[CallContextAkka])
case class InboundGetAccount(payload: Option[InboundAccountDec2018], callContext: Option[CallContextAkka])
case class InboundGetCoreBankAccounts(payload: List[InternalInboundCoreAccount], callContext: Option[CallContextAkka])
case class InboundGetCustomersByUserId(payload: List[InternalCustomer], callContext: Option[CallContextAkka])



case class Bank(bankId: BankId,
                shortName: String,
                fullName: String,
                logoUrl: String,
                websiteUrl: String,
                bankRoutingScheme: String,
                bankRoutingAddress: String
               )

case class BankAkka(b: Bank) extends BankTrait {
  override def bankId = b.bankId
  override def fullName = b.fullName
  override def shortName = b.shortName
  override def logoUrl = b.logoUrl
  override def websiteUrl = b.websiteUrl
  override def bankRoutingScheme = b.bankRoutingScheme
  override def bankRoutingAddress = b.bankRoutingAddress
  override def swiftBic = ""
  override def nationalIdentifier: String = ""
}

case class InboundAccountDec2018(
                                 bankId: String,
                                 branchId: String,
                                 accountId: String,
                                 accountNumber: String,
                                 accountType: String,
                                 balanceAmount: String,
                                 balanceCurrency: String,
                                 owners: List[String],
                                 viewsToGenerate: List[String],
                                 bankRoutingScheme: String,
                                 bankRoutingAddress: String,
                                 branchRoutingScheme: String,
                                 branchRoutingAddress: String,
                                 accountRoutingScheme: String,
                                 accountRoutingAddress: String,
                                 accountRouting: List[AccountRouting],
                                 accountRules: List[AccountRule]
                                )

case class BankAccountDec2018(r: InboundAccountDec2018) extends BankAccount {

  def accountId: AccountId = AccountId(r.accountId)
  def accountType: String = r.accountType
  def balance: BigDecimal = BigDecimal(r.balanceAmount)
  def currency: String = r.balanceCurrency
  def name: String = r.owners.head
  // Note: swift_bic--> swiftBic, but it extends from BankAccount
  def swift_bic: Option[String] = Some("swift_bic")
  // Note: deprecated, extends from BankAccount
  def iban: Option[String] = Some("iban")
  def number: String = r.accountNumber
  def bankId: BankId = BankId(r.bankId)
  def lastUpdate: Date = APIUtil.DateWithMsFormat.parse(today.getTime.toString)
  def accountHolder: String = r.owners.head

  // Fields modifiable from OBP are stored in mapper
  def label: String = (for {
    d <- MappedBankAccountData.find(By(MappedBankAccountData.accountId, r.accountId))
  } yield {
    d.getLabel
  }).getOrElse(r.accountNumber)

  def accountRoutingScheme: String = r.accountRoutingScheme
  def accountRoutingAddress: String = r.accountRoutingAddress
  def accountRoutings: List[AccountRouting] = List()
  def branchId: String = r.branchId

  def accountRules: List[AccountRule] = r.accountRules

}

case class InternalInboundCoreAccount(
                                       id : String,
                                       label : String,
                                       bankId : String,
                                       accountType: String,
                                       accountRoutings: List[AccountRouting]
                                     )

case class InternalCustomer(
                             customerId: String,
                             bankId: String,
                             number: String,
                             legalName: String,
                             mobileNumber: String,
                             email: String,
                             faceImage: CustomerFaceImage,
                             dateOfBirth: Date,
                             relationshipStatus: String,
                             dependents: Integer,
                             dobOfDependents: List[Date],
                             highestEducationAttained: String,
                             employmentStatus: String,
                             creditRating: CreditRating,
                             creditLimit: CreditLimit,
                             kycStatus: lang.Boolean,
                             lastOkDate: Date
                           )

case class AkkaDec2018Customer(
                        customerId: String,
                        bankId: String,
                        number: String,
                        legalName: String,
                        mobileNumber: String,
                        email: String,
                        faceImage: CustomerFaceImage,
                        dateOfBirth: Date,
                        relationshipStatus: String,
                        dependents: Integer,
                        dobOfDependents: List[Date],
                        highestEducationAttained: String,
                        employmentStatus: String,
                        creditRating: CreditRating,
                        creditLimit: CreditLimit,
                        kycStatus: lang.Boolean,
                        lastOkDate: Date,
                        title: String = "", //These new fields for V310, not from Connector for now. 
                        branchId: String = "", //These new fields for V310, not from Connector for now. 
                        nameSuffix: String = "", //These new fields for V310, not from Connector for now. 
                      ) extends Customer



object InboundTransformerDec2018 {
  def toCustomer(customer : InternalCustomer): Customer = {
    AkkaDec2018Customer(
      customerId = customer.customerId,
      bankId = customer.bankId,
      number = customer.number,
      legalName = customer.legalName,
      mobileNumber = customer.mobileNumber,
      email = customer.email,
      faceImage = customer.faceImage,
      dateOfBirth = customer.dateOfBirth,
      relationshipStatus = customer.relationshipStatus,
      dependents = customer.dependents,
      dobOfDependents = customer.dobOfDependents,
      highestEducationAttained = customer.highestEducationAttained,
      employmentStatus = customer.employmentStatus,
      creditRating = customer.creditRating,
      creditLimit = customer.creditLimit,
      kycStatus = customer.kycStatus,
      lastOkDate = customer.lastOkDate,
    )
  }

  def toCustomers(customers : List[InternalCustomer]) : List[Customer] = {
    customers.map(toCustomer)
  }
}