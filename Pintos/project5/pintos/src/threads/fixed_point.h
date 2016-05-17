#define F (1 << 14)                 // shifting
#define INT_MAX ((1 << 31)- 1 )
#define INT_MIN (-(1 << 31))

// x and y denote fixed_point numbers in 17.14 format
// n is an integer

int int_to_fp(int n);               // integer -> fixed pointer
int fp_to_int_round(int x);         // fp -> int ( round )
int fp_to_int(int x);               // fp -> int (  );
int add_fp(int x, int y);           // fp + fp
int add_mixed(int x, int n);        // fp + int
int sub_fp(int x, int y);           // fp - fp
int sub_mixed(int x, int n);        // fp - int
int mult_fp(int x, int y);          // fp * fp
int mult_mixed(int x, int n);       // fp * int
int div_fp(int x, int y);           // fp / fp
int div_mixed(int x, int n);        // fp / int

int int_to_fp(int n)                // integer -> fixed pointer
{
	return n*F;
}

int fp_to_int(int x)                // fp -> int (  );
{
	return x/F;
}

int fp_to_int_round(int x)          // fp -> int ( round )
{
	if(x >= 0)
		return (x + F/2)/F;
	else return (x - F/2)/F;  
}
int add_fp(int x, int y)            // fp + fp
{
	return x + y;
}

int add_mixed(int x, int n)         // fp + int
{
	return x + n*F;
}

int sub_fp(int x,int y)             // fp - fp
{
	return x-y;
}

int sub_mixed(int x, int n)         // fp - int
{
	return x- n*F;
}

int mult_fp(int x, int y)           // fp * fp
{
	return ((int64_t)x)*y/F;
}

int mult_mixed(int x, int n)        // fp * int
{
	return x*n;
}

int div_fp(int x, int y)            // divide fp and fp
{
	return ((int64_t)x)*F/y;
}

int div_mixed(int x, int n)         // divide fp and integer
{
	return x/n;
}
